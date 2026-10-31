package com.substringtechnologies.jpaadmin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.substringtechnologies.jpaadmin.annotations.ActionScope;
import com.substringtechnologies.jpaadmin.metadata.ActionMetadata;
import com.substringtechnologies.jpaadmin.metadata.EntityMetadata;
import com.substringtechnologies.jpaadmin.metadata.FieldMetadata;
import com.substringtechnologies.jpaadmin.metadata.FieldType;
import com.substringtechnologies.jpaadmin.metadata.InlineMetadata;
import com.substringtechnologies.jpaadmin.metadata.JpaMetamodelScanner;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.persistence.criteria.*;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.*;

@Service
@Transactional
public class DynamicCrudService {

    private final EntityManager entityManager;
    private final JpaMetamodelScanner scanner;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;
    private Validator validator;

    public DynamicCrudService(EntityManager entityManager,
                              JpaMetamodelScanner scanner,
                              ObjectMapper objectMapper,
                              ApplicationContext applicationContext) {
        this.entityManager = entityManager;
        this.scanner = scanner;
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> listRecords(
            String entityName,
            Pageable pageable,
            String searchQuery,
            Map<String, String> filters) {

        EntityMetadata metadata = requireMetadata(entityName);
        Class<?> entityClass = loadClass(metadata.getJavaClassName());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> cq = cb.createQuery();
        Root<?> root = cq.from(entityClass);

        List<Predicate> predicates = buildPredicates(cb, root, metadata, searchQuery, filters);
        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        // Sorting
        applySorting(cb, cq, root, metadata, pageable.getSort());

        var query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Object> rawResults = query.getResultList();
        long totalCount = executeCountQuery(entityClass, metadata, searchQuery, filters);

        List<Map<String, Object>> content = new ArrayList<>();
        for (Object item : rawResults) {
            content.add(serializeEntity(item, metadata, false));
        }

        return new PageResult<>(content, totalCount, pageable.getPageNumber(), pageable.getPageSize());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRecord(String entityName, Object rawId) {
        EntityMetadata metadata = requireMetadata(entityName);
        Class<?> entityClass = loadClass(metadata.getJavaClassName());
        Object id = convertId(rawId, metadata);

        Object entity = entityManager.find(entityClass, id);
        if (entity == null) {
            throw new NoSuchElementException(metadata.getDisplayName() + " with ID " + id + " not found");
        }
        return serializeEntity(entity, metadata, true);
    }

    public Map<String, Object> createRecord(String entityName, Map<String, Object> payload) {
        EntityMetadata metadata = requireMetadata(entityName);
        if (!metadata.isCreatable() || metadata.isReadOnly()) {
            throw new UnsupportedOperationException("Creation is disabled for " + metadata.getDisplayName());
        }

        Object entity = instantiate(loadClass(metadata.getJavaClassName()));
        Map<String, String> errors = new LinkedHashMap<>();
        bindPayload(entity, payload, metadata, false, errors, "");
        InlineChanges changes = bindInlines(entity, payload, metadata, errors);
        validate(entity, "", errors);
        if (!errors.isEmpty()) {
            throw new AdminValidationException(errors);
        }
        entityManager.persist(entity);
        changes.apply(entityManager);
        entityManager.flush();
        return serializeEntity(entity, metadata, true);
    }

    private Object instantiate(Class<?> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(type.getSimpleName() + " needs a no-argument constructor", e);
        }
    }

    private Validator validator() {
        if (validator == null) {
            validator = applicationContext.getBeanProvider(Validator.class)
                    .getIfAvailable(() -> Validation.buildDefaultValidatorFactory().getValidator());
        }
        return validator;
    }

    private void validate(Object entity, String prefix, Map<String, String> errors) {
        for (ConstraintViolation<Object> violation : validator().validate(entity)) {
            errors.putIfAbsent(prefix + violation.getPropertyPath(), violation.getMessage());
        }
    }

    /** New and removed inline children, applied once the parent is managed so back-references resolve. */
    private record InlineChanges(List<Object> added, List<Object> removed) {
        void apply(EntityManager em) {
            added.forEach(em::persist);
            removed.forEach(child -> { if (em.contains(child)) em.remove(child); });
        }
    }

    @SuppressWarnings("unchecked")
    private InlineChanges bindInlines(Object parent, Map<String, Object> payload, EntityMetadata metadata, Map<String, String> errors) {
        InlineChanges changes = new InlineChanges(new ArrayList<>(), new ArrayList<>());
        for (InlineMetadata inline : metadata.getInlines()) {
            String prop = inline.getPropertyName();
            // Only parent-owned one-to-many collections can be edited; removing a many-to-many row would delete shared data.
            if (!(payload.get(prop) instanceof List<?> rows) || inline.getMappedBy() == null || inline.getMappedBy().isEmpty()) continue;
            EntityMetadata childMeta = scanner.getMetadata(inline.getTargetEntity());
            if (childMeta == null) continue;
            if (inline.getLimit() > 0 && rows.size() > inline.getLimit()) {
                errors.put(prop, "At most " + inline.getLimit() + " rows are allowed");
                continue;
            }
            Collection<Object> collection = (Collection<Object>) getFieldValue(parent, prop);
            if (collection == null) {
                collection = new ArrayList<>();
                setFieldValue(parent, prop, collection);
            }
            String childPk = childMeta.getPrimaryKeyField();
            Map<String, Object> existing = new LinkedHashMap<>();
            for (Object child : collection) existing.put(String.valueOf(getFieldValue(child, childPk)), child);

            Set<Object> kept = Collections.newSetFromMap(new IdentityHashMap<>());
            for (int i = 0; i < rows.size(); i++) {
                if (!(rows.get(i) instanceof Map<?, ?> rawRow)) continue;
                Map<String, Object> row = new HashMap<>((Map<String, Object>) rawRow);
                row.remove(inline.getMappedBy());
                String rowPrefix = prop + "[" + i + "].";
                Object rowId = row.get(childPk);
                Object child = rowId == null ? null : existing.get(String.valueOf(rowId));
                if (child == null) {
                    if (!inline.isCanAdd()) {
                        errors.put(prop, "New " + inline.getTitle().toLowerCase() + " cannot be added");
                        continue;
                    }
                    child = instantiate(loadClass(childMeta.getJavaClassName()));
                    bindPayload(child, row, childMeta, false, errors, rowPrefix);
                    setFieldValue(child, inline.getMappedBy(), parent);
                    collection.add(child);
                    changes.added().add(child);
                } else {
                    bindPayload(child, row, childMeta, true, errors, rowPrefix);
                }
                kept.add(child);
                validate(child, rowPrefix, errors);
            }
            for (Object child : new ArrayList<>(collection)) {
                if (kept.contains(child)) continue;
                if (!inline.isCanDelete()) {
                    errors.put(prop, inline.getTitle() + " cannot be removed");
                    continue;
                }
                collection.remove(child);
                changes.removed().add(child);
            }
        }
        return changes;
    }

    public Map<String, Object> updateRecord(String entityName, Object rawId, Map<String, Object> payload) {
        EntityMetadata metadata = requireMetadata(entityName);
        if (!metadata.isUpdatable() || metadata.isReadOnly()) {
            throw new UnsupportedOperationException("Update is disabled for " + metadata.getDisplayName());
        }

        Class<?> entityClass = loadClass(metadata.getJavaClassName());
        Object id = convertId(rawId, metadata);

        Object entity = entityManager.find(entityClass, id);
        if (entity == null) {
            throw new NoSuchElementException(metadata.getDisplayName() + " with ID " + id + " not found");
        }

        Map<String, String> errors = new LinkedHashMap<>();
        bindPayload(entity, payload, metadata, true, errors, "");
        InlineChanges changes = bindInlines(entity, payload, metadata, errors);
        validate(entity, "", errors);
        if (!errors.isEmpty()) {
            // Throwing rolls back the transaction, so the rejected values on this managed entity never flush.
            throw new AdminValidationException(errors);
        }
        changes.apply(entityManager);
        entityManager.flush();
        return serializeEntity(entity, metadata, true);
    }

    public void deleteRecord(String entityName, Object rawId) {
        EntityMetadata metadata = requireMetadata(entityName);
        if (!metadata.isDeletable() || metadata.isReadOnly()) {
            throw new UnsupportedOperationException("Deletion is disabled for " + metadata.getDisplayName());
        }

        Class<?> entityClass = loadClass(metadata.getJavaClassName());
        Object id = convertId(rawId, metadata);

        Object entity = entityManager.find(entityClass, id);
        if (entity != null) {
            entityManager.remove(entity);
            entityManager.flush();
        }
    }

    public int batchDelete(String entityName, List<Object> rawIds) {
        EntityMetadata metadata = requireMetadata(entityName);
        if (!metadata.isDeletable() || metadata.isReadOnly()) {
            throw new UnsupportedOperationException("Deletion is disabled for " + metadata.getDisplayName());
        }

        int count = 0;
        for (Object id : rawIds) {
            deleteRecord(entityName, id);
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> lookup(String entityName, String query, int limit) {
        EntityMetadata metadata = requireMetadata(entityName);
        Class<?> entityClass = loadClass(metadata.getJavaClassName());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> cq = cb.createQuery();
        Root<?> root = cq.from(entityClass);

        String pkField = metadata.getPrimaryKeyField();
        String displayProp = null;

        // Find primary display property for DB filtering
        for (String candidate : new String[]{"name", "title", "label", "fullName", "displayName", "username", "code"}) {
            if (metadata.getField(candidate) != null) {
                displayProp = candidate;
                break;
            }
        }
        if (displayProp == null) {
            for (FieldMetadata f : metadata.getFields()) {
                if (!f.isPrimaryKey() && f.getFieldType() == FieldType.STRING && !f.isSensitive()) {
                    displayProp = f.getName();
                    break;
                }
            }
        }
        if (displayProp == null) displayProp = pkField;

        if (query != null && !query.trim().isEmpty()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            if (displayProp != null) {
                predicates.add(cb.like(cb.lower(root.get(displayProp).as(String.class)), q));
            }
            if (pkField != null && !pkField.equals(displayProp)) {
                predicates.add(cb.like(cb.lower(root.get(pkField).as(String.class)), q));
            }
            if (!predicates.isEmpty()) {
                cq.where(cb.or(predicates.toArray(new Predicate[0])));
            }
        }

        var typedQuery = entityManager.createQuery(cq);
        typedQuery.setMaxResults(limit > 0 ? limit : 20);

        List<Object> results = typedQuery.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object item : results) {
            Map<String, Object> map = new HashMap<>();
            Object idVal = getFieldValue(item, pkField);
            String label = resolveEntityLabel(item, metadata, null);
            map.put("id", idVal);
            map.put("label", (label != null && !label.isEmpty()) ? label : String.valueOf(idVal));
            list.add(map);
        }
        return list;
    }

    public Object executeAction(String entityName, String actionId, List<Object> rawIds) {
        EntityMetadata metadata = requireMetadata(entityName);
        Class<?> entityClass = loadClass(metadata.getJavaClassName());

        ActionMetadata action = metadata.getActions().stream()
                .filter(a -> a.getId().equalsIgnoreCase(actionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Action " + actionId + " not found on " + entityName));

        List<Object> entities = new ArrayList<>();
        for (Object rawId : rawIds) {
            Object id = convertId(rawId, metadata);
            Object entity = entityManager.find(entityClass, id);
            if (entity != null) entities.add(entity);
        }

        try {
            Method method = entityClass.getDeclaredMethod(action.getMethodName());
            method.setAccessible(true);

            if (action.isStatic()) {
                // If static method, pass list of selected entities
                return method.invoke(null, entities);
            } else {
                // Instance method, invoke on each selected entity
                List<Object> results = new ArrayList<>();
                for (Object ent : entities) {
                    results.add(method.invoke(ent));
                }
                entityManager.flush();
                return results;
            }
        } catch (NoSuchMethodException e) {
            // Check if there is a method accepting List
            try {
                Method method = entityClass.getDeclaredMethod(action.getMethodName(), List.class);
                method.setAccessible(true);
                return method.invoke(null, entities);
            } catch (Exception ex) {
                throw new RuntimeException("Action method execution failed: " + ex.getMessage(), ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("Action execution error: " + e.getMessage(), e);
        }
    }

    // --- Helper Methods ---

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<?> root, EntityMetadata metadata, String search, Map<String, String> filters) {
        List<Predicate> predicates = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            List<Predicate> searchPreds = new ArrayList<>();
            String pattern = "%" + search.trim().toLowerCase() + "%";
            for (FieldMetadata field : metadata.getSearchableFields()) {
                if (field.getFieldType() == FieldType.STRING && !field.isComputed() && !field.isSensitive()) {
                    searchPreds.add(cb.like(cb.lower(root.get(field.getName()).as(String.class)), pattern));
                }
            }
            if (searchPreds.isEmpty()) {
                // Default search on primary string field
                for (FieldMetadata f : metadata.getFields()) {
                    if (f.getFieldType() == FieldType.STRING && !f.isSensitive() && !f.isComputed()) {
                        searchPreds.add(cb.like(cb.lower(root.get(f.getName()).as(String.class)), pattern));
                    }
                }
            }
            if (!searchPreds.isEmpty()) {
                predicates.add(cb.or(searchPreds.toArray(new Predicate[0])));
            }
        }

        if (filters != null) {
            filters.forEach((rawKey, val) -> {
                Predicate predicate = buildFilter(cb, root, metadata, rawKey, val);
                if (predicate != null) predicates.add(predicate);
            });
        }

        return predicates;
    }

    /**
     * Supports {@code field=value} plus {@code field__gte} / {@code field__lte} range bounds on numbers and dates.
     * Text matches case-insensitively by substring; relations match by the related record's id.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildFilter(CriteriaBuilder cb, Root<?> root, EntityMetadata metadata, String rawKey, String val) {
        if (val == null || val.trim().isEmpty()) return null;
        String op = rawKey.endsWith("__gte") ? "gte" : rawKey.endsWith("__lte") ? "lte" : "eq";
        String key = op.equals("eq") ? rawKey : rawKey.substring(0, rawKey.length() - 5);
        FieldMetadata field = metadata.getField(key);
        if (field == null || field.isComputed() || field.isSensitive()) return null;

        try {
            switch (field.getFieldType()) {
                case RELATION_MANY_TO_ONE, RELATION_ONE_TO_ONE -> {
                    EntityMetadata relMeta = scanner.getMetadata(field.getLookupTargetEntity());
                    if (relMeta == null) return null;
                    return cb.equal(root.get(key).get(relMeta.getPrimaryKeyField()), convertId(val.trim(), relMeta));
                }
                case RELATION_ONE_TO_MANY, RELATION_MANY_TO_MANY, EMBEDDED, UNKNOWN -> {
                    return null;
                }
                case STRING -> {
                    if (!op.equals("eq")) return null;
                    return cb.like(cb.lower(root.get(key).as(String.class)), "%" + val.trim().toLowerCase() + "%");
                }
                default -> {
                    String value = val.trim();
                    if (field.getFieldType() == FieldType.DATE_TIME && value.length() == 10) {
                        // A date-only bound covers the whole day.
                        value += op.equals("lte") ? "T23:59:59.999999999" : "T00:00:00";
                        if (op.equals("eq")) {
                            Path<Comparable> path = root.get(key);
                            Comparable from = (Comparable) parseFieldValue(val.trim() + "T00:00:00", field, root.getJavaType());
                            Comparable to = (Comparable) parseFieldValue(val.trim() + "T23:59:59.999999999", field, root.getJavaType());
                            return cb.between(path, from, to);
                        }
                    }
                    Object parsed = parseFieldValue(value, field, root.getJavaType());
                    if (parsed == null) return null;
                    if (op.equals("eq")) return cb.equal(root.get(key), parsed);
                    if (!(parsed instanceof Comparable comparable)) return null;
                    Path<Comparable> path = root.get(key);
                    return op.equals("gte") ? cb.greaterThanOrEqualTo(path, comparable) : cb.lessThanOrEqualTo(path, comparable);
                }
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid filter value for " + field.getLabel() + ": " + val);
        }
    }

    private void applySorting(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<?> root, EntityMetadata metadata, Sort sort) {
        List<Order> orders = new ArrayList<>();
        if (sort != null && sort.isSorted()) {
            for (Sort.Order o : sort) {
                if (metadata.hasField(o.getProperty())) {
                    FieldMetadata field = metadata.getField(o.getProperty());
                    if (field != null && !field.isComputed()) {
                        orders.add(o.isAscending() ? cb.asc(root.get(o.getProperty())) : cb.desc(root.get(o.getProperty())));
                    }
                }
            }
        }
        if (orders.isEmpty()) {
            String defaultSort = metadata.getDefaultSortBy();
            if (metadata.hasField(defaultSort)) {
                FieldMetadata defField = metadata.getField(defaultSort);
                if (defField != null && !defField.isComputed()) {
                    orders.add(metadata.getDefaultSortDir() == com.substringtechnologies.jpaadmin.annotations.SortDir.ASC ?
                            cb.asc(root.get(defaultSort)) : cb.desc(root.get(defaultSort)));
                }
            }
        }
        cq.orderBy(orders);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countAll() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (EntityMetadata metadata : scanner.getAllMetadata()) {
            Class<?> entityClass = loadClass(metadata.getJavaClassName());
            counts.put(metadata.getEntityName(), executeCountQuery(entityClass, metadata, null, Map.of()));
        }
        return counts;
    }

    private long executeCountQuery(Class<?> entityClass, EntityMetadata metadata, String search, Map<String, String> filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<?> root = countCq.from(entityClass);
        countCq.select(cb.count(root));

        List<Predicate> countPredicates = buildPredicates(cb, root, metadata, search, filters);
        if (!countPredicates.isEmpty()) {
            countCq.where(countPredicates.toArray(new Predicate[0]));
        }
        return entityManager.createQuery(countCq).getSingleResult();
    }

    public Map<String, Object> serializeEntity(Object entity, EntityMetadata metadata, boolean includeInlines) {
        if (entity == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();

        for (FieldMetadata fieldMeta : metadata.getFields()) {
            String prop = fieldMeta.getName();

            // Computed virtual method
            if (fieldMeta.isComputed()) {
                try {
                    String getter = "get" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
                    Method m = entity.getClass().getMethod(getter);
                    map.put(prop, m.invoke(entity));
                } catch (Exception e) {
                    try {
                        String isGetter = "is" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
                        Method m = entity.getClass().getMethod(isGetter);
                        map.put(prop, m.invoke(entity));
                    } catch (Exception ex) {
                        map.put(prop, null);
                    }
                }
                continue;
            }

            if (fieldMeta.getFieldType() == FieldType.RELATION_MANY_TO_MANY && includeInlines
                    && metadata.getInlines().stream().noneMatch(i -> i.getPropertyName().equals(prop))) {
                map.put(prop, serializeReferences(getFieldValue(entity, prop), fieldMeta));
                continue;
            }
            if (fieldMeta.getFieldType() == FieldType.RELATION_ONE_TO_MANY || fieldMeta.getFieldType() == FieldType.RELATION_MANY_TO_MANY) {
                continue;
            }

            Object val = getFieldValue(entity, prop);

            // Sensitive field masking
            if (fieldMeta.isSensitive() && val != null) {
                map.put(prop, "••••••••");
                continue;
            }

            // Relationship serialization
            if (fieldMeta.getFieldType() == FieldType.RELATION_MANY_TO_ONE || fieldMeta.getFieldType() == FieldType.RELATION_ONE_TO_ONE) {
                if (val != null) {
                    Map<String, Object> relMap = new LinkedHashMap<>();
                    EntityMetadata relMeta = scanner.getMetadata(fieldMeta.getLookupTargetEntity());
                    String relPk = (relMeta != null) ? relMeta.getPrimaryKeyField() : "id";
                    Object relId = getFieldValue(val, relPk);
                    relMap.put("id", relId);

                    String label = resolveEntityLabel(val, relMeta, fieldMeta.getLookupProperty());
                    relMap.put("label", (label != null && !label.isEmpty()) ? label : String.valueOf(relId));
                    map.put(prop, relMap);
                } else {
                    map.put(prop, null);
                }
                continue;
            }

            map.put(prop, val);
        }

        // Inline relationship serialization (e.g. Order Items inside Order)
        if (includeInlines && !metadata.getInlines().isEmpty()) {
            for (InlineMetadata inline : metadata.getInlines()) {
                Object relCol = getFieldValue(entity, inline.getPropertyName());
                if (relCol instanceof Collection<?> col) {
                    EntityMetadata childMeta = scanner.getMetadata(inline.getTargetEntity());
                    List<Map<String, Object>> childList = new ArrayList<>();
                    if (childMeta != null) {
                        for (Object child : col) {
                            childList.add(serializeEntity(child, childMeta, false));
                        }
                    }
                    map.put(inline.getPropertyName(), childList);
                }
            }
        }

        return map;
    }

    private static final int MAX_SERIALIZED_REFERENCES = 200;

    private List<Map<String, Object>> serializeReferences(Object value, FieldMetadata fieldMeta) {
        List<Map<String, Object>> refs = new ArrayList<>();
        if (!(value instanceof Collection<?> collection)) return refs;
        EntityMetadata relMeta = scanner.getMetadata(fieldMeta.getLookupTargetEntity());
        String relPk = relMeta != null ? relMeta.getPrimaryKeyField() : "id";
        for (Object item : collection) {
            if (refs.size() >= MAX_SERIALIZED_REFERENCES) break;
            Object id = getFieldValue(item, relPk);
            String label = resolveEntityLabel(item, relMeta, null);
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("id", id);
            ref.put("label", label != null && !label.isEmpty() ? label : String.valueOf(id));
            refs.add(ref);
        }
        return refs;
    }

    /**
     * Daily creation counts for entities with a creation timestamp (createdAt, createdDate, ...), oldest day first.
     * Entities whose timestamp can't be grouped by the database are left out rather than failing the dashboard.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> creationTimeline(int days) {
        int span = Math.max(1, Math.min(days, 365));
        LocalDate start = LocalDate.now().minusDays(span - 1L);
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < span; i++) labels.add(start.plusDays(i).toString());

        Map<String, List<Long>> series = new LinkedHashMap<>();
        for (EntityMetadata metadata : scanner.getAllMetadata()) {
            FieldMetadata stamp = metadata.getFields().stream()
                    .filter(f -> !f.isComputed() && isCreationTimestampField(f.getName())
                            && (f.getFieldType() == FieldType.DATE_TIME || f.getFieldType() == FieldType.DATE))
                    .findFirst().orElse(null);
            if (stamp == null) continue;
            Class<?> entityClass = loadClass(metadata.getJavaClassName());
            Field field = findDeclaredField(entityClass, stamp.getName());
            if (field == null) continue;
            Object since = convertToDateTime(start, field.getType());
            if (since == null) continue;
            try {
                // Names come from the JPA metamodel, never from request input.
                String hql = "select cast(e." + stamp.getName() + " as LocalDate), count(e) from " + metadata.getEntityName()
                        + " e where e." + stamp.getName() + " >= :since group by cast(e." + stamp.getName() + " as LocalDate)";
                List<Object[]> rows = entityManager.createQuery(hql, Object[].class).setParameter("since", since).getResultList();
                Long[] counts = new Long[span];
                Arrays.fill(counts, 0L);
                for (Object[] row : rows) {
                    LocalDate day = row[0] instanceof LocalDate d ? d : LocalDate.parse(String.valueOf(row[0]).substring(0, 10));
                    int index = (int) (day.toEpochDay() - start.toEpochDay());
                    if (index >= 0 && index < span) counts[index] = ((Number) row[1]).longValue();
                }
                series.put(metadata.getEntityName(), Arrays.asList(counts));
            } catch (RuntimeException ignored) {
                // Some dialects or java.util.Date mappings can't cast to a date; skip that entity.
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", labels);
        result.put("series", series);
        return result;
    }

    private boolean isCreationTimestampField(String fieldName) {
        if (fieldName == null) return false;
        String normalized = fieldName.toLowerCase().replace("_", "").replace("-", "");
        return normalized.equals("createdat")
                || normalized.equals("createddate")
                || normalized.equals("createdon")
                || normalized.equals("creationdate")
                || normalized.equals("creationtime")
                || normalized.equals("createtime")
                || normalized.equals("createddatetime");
    }

    private void bindPayload(Object entity, Map<String, Object> payload, EntityMetadata metadata, boolean isUpdate,
                             Map<String, String> errors, String prefix) {
        for (FieldMetadata fieldMeta : metadata.getFields()) {
            String prop = fieldMeta.getName();
            if (fieldMeta.isPrimaryKey() && (fieldMeta.isGeneratedValue() || isUpdate)) continue;
            if (fieldMeta.isComputed() || !payload.containsKey(prop)) continue;
            // On update, skip read-only and creation timestamp fields (like createdAt)
            if (isUpdate && (fieldMeta.isReadOnly() || isCreationTimestampField(prop))) continue;
            if (fieldMeta.getFieldType() == FieldType.RELATION_ONE_TO_MANY) continue;

            Object rawVal = payload.get(prop);
            try {
                switch (fieldMeta.getFieldType()) {
                    case RELATION_MANY_TO_ONE, RELATION_ONE_TO_ONE -> {
                        Object foreignId = (rawVal instanceof Map<?, ?> m) ? m.get("id") : rawVal;
                        setFieldValue(entity, prop, foreignId == null || "".equals(foreignId) ? null : reference(fieldMeta, foreignId));
                    }
                    case RELATION_MANY_TO_MANY -> {
                        if (fieldMeta.isReadOnly()) continue;
                        bindManyToMany(entity, fieldMeta, rawVal);
                    }
                    default -> setFieldValue(entity, prop, parseFieldValue(rawVal, fieldMeta, entity.getClass()));
                }
            } catch (EntityNotFoundException | NoSuchElementException e) {
                errors.put(prefix + prop, "The selected record no longer exists");
            } catch (RuntimeException e) {
                errors.put(prefix + prop, invalidValueMessage(fieldMeta));
            }
        }
    }

    private String invalidValueMessage(FieldMetadata field) {
        return switch (field.getFieldType()) {
            case NUMBER -> "Enter a valid number";
            case DATE -> "Enter a valid date";
            case DATE_TIME -> "Enter a valid date and time";
            case ENUM -> "Choose one of the listed options";
            default -> "This value is not valid";
        };
    }

    private Object reference(FieldMetadata fieldMeta, Object rawId) {
        EntityMetadata relMeta = scanner.getMetadata(fieldMeta.getLookupTargetEntity());
        if (relMeta == null) throw new NoSuchElementException(fieldMeta.getLookupTargetEntity());
        Object found = entityManager.find(loadClass(relMeta.getJavaClassName()), convertId(rawId, relMeta));
        if (found == null) throw new NoSuchElementException(String.valueOf(rawId));
        return found;
    }

    @SuppressWarnings("unchecked")
    private void bindManyToMany(Object entity, FieldMetadata fieldMeta, Object rawVal) {
        List<Object> targets = new ArrayList<>();
        if (rawVal instanceof Collection<?> values) {
            for (Object value : values) {
                Object id = value instanceof Map<?, ?> m ? m.get("id") : value;
                if (id != null && !"".equals(id)) targets.add(reference(fieldMeta, id));
            }
        }
        Object current = getFieldValue(entity, fieldMeta.getName());
        if (current instanceof Collection<?> collection) {
            // Mutate in place so Hibernate keeps tracking the persistent collection.
            ((Collection<Object>) collection).clear();
            ((Collection<Object>) collection).addAll(targets);
        } else {
            Field field = findDeclaredField(entity.getClass(), fieldMeta.getName());
            boolean isSet = field != null && Set.class.isAssignableFrom(field.getType());
            setFieldValue(entity, fieldMeta.getName(), isSet ? new LinkedHashSet<>(targets) : targets);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object parseFieldValue(Object rawVal, FieldMetadata fieldMeta, Class<?> entityClass) {
        if (rawVal == null) return null;
        String str = rawVal.toString().trim();
        if (str.isEmpty()) return null;

        return switch (fieldMeta.getFieldType()) {
            case NUMBER -> {
                String jType = fieldMeta.getJavaType();
                if ("Long".equalsIgnoreCase(jType) || "long".equalsIgnoreCase(jType)) yield Long.parseLong(str.replaceAll("[^0-9.-]", ""));
                if ("Integer".equalsIgnoreCase(jType) || "int".equalsIgnoreCase(jType)) yield Integer.parseInt(str.replaceAll("[^0-9.-]", ""));
                if ("BigDecimal".equalsIgnoreCase(jType)) yield new BigDecimal(str);
                if ("Double".equalsIgnoreCase(jType) || "double".equalsIgnoreCase(jType)) yield Double.parseDouble(str);
                if ("Float".equalsIgnoreCase(jType) || "float".equalsIgnoreCase(jType)) yield Float.parseFloat(str);
                if ("BigInteger".equalsIgnoreCase(jType)) yield new BigInteger(str);
                yield Double.parseDouble(str);
            }
            case BOOLEAN -> Boolean.parseBoolean(str);
            case DATE -> {
                if (str.length() >= 10 && str.charAt(4) == '-' && str.charAt(7) == '-') {
                    yield LocalDate.parse(str.substring(0, 10));
                }
                yield LocalDate.parse(str);
            }
            case DATE_TIME -> {
                Field f = (entityClass != null) ? findDeclaredField(entityClass, fieldMeta.getName()) : null;
                Class<?> fType = (f != null) ? f.getType() : LocalDateTime.class;
                yield convertToDateTime(str, fType);
            }
            case ENUM -> {
                if (entityClass != null) {
                    Field f = findDeclaredField(entityClass, fieldMeta.getName());
                    if (f != null && f.getType().isEnum()) {
                        try {
                            yield Enum.valueOf((Class<Enum>) f.getType(), str);
                        } catch (Exception ignored) {}
                    }
                }
                yield str;
            }
            default -> str;
        };
    }

    private Object convertId(Object rawId, EntityMetadata metadata) {
        FieldMetadata pkField = metadata.getField(metadata.getPrimaryKeyField());
        if (pkField == null) return rawId;
        String type = pkField.getJavaType();
        String str = rawId.toString();

        if ("Long".equalsIgnoreCase(type) || "long".equalsIgnoreCase(type)) {
            return Long.parseLong(str);
        } else if ("Integer".equalsIgnoreCase(type) || "int".equalsIgnoreCase(type)) {
            return Integer.parseInt(str);
        } else if ("UUID".equalsIgnoreCase(type)) {
            return UUID.fromString(str);
        }
        return str;
    }

    private Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) return null;
        try {
            String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                Method m = obj.getClass().getMethod(getter);
                return m.invoke(obj);
            } catch (NoSuchMethodException ignored) {}

            String isGetter = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                Method m = obj.getClass().getMethod(isGetter);
                return m.invoke(obj);
            } catch (NoSuchMethodException ignored) {}

            Field f = findDeclaredField(obj.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                return f.get(obj);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setFieldValue(Object obj, String fieldName, Object value) {
        if (obj == null || fieldName == null) return;
        try {
            Field f = findDeclaredField(obj.getClass(), fieldName);
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

            // 1. Try finding any setter with 1 parameter matching setterName
            Method setter = null;
            if (f != null) {
                try {
                    setter = obj.getClass().getMethod(setterName, f.getType());
                } catch (NoSuchMethodException ignored) {}
            }
            if (setter == null) {
                for (Method m : obj.getClass().getMethods()) {
                    if (m.getName().equalsIgnoreCase(setterName) && m.getParameterCount() == 1) {
                        setter = m;
                        break;
                    }
                }
            }

            if (setter != null) {
                try {
                    Class<?> paramType = setter.getParameterTypes()[0];
                    Object finalVal = convertValueToTargetType(value, paramType);
                    setter.setAccessible(true);
                    setter.invoke(obj, finalVal);
                    return;
                } catch (Exception ignored) {
                    // Fall back to direct field reflection if setter invocation fails
                }
            }

            // 2. Direct field reflection fallback
            if (f != null) {
                try {
                    f.setAccessible(true);
                    Object finalVal = convertValueToTargetType(value, f.getType());
                    f.set(obj, finalVal);
                    return;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // If creation timestamp field, do not fail
            if (isCreationTimestampField(fieldName)) {
                return;
            }
            throw new RuntimeException("Failed to set field " + fieldName + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertValueToTargetType(Object value, Class<?> targetType) {
        if (value == null || targetType == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }

        Class<?> unwrappedType = targetType.isPrimitive() ? wrapPrimitive(targetType) : targetType;
        if (unwrappedType.isInstance(value)) {
            return value;
        }

        String str = value.toString().trim();
        if (unwrappedType == String.class) {
            return str;
        }
        if (str.isEmpty()) {
            return null;
        }

        if (unwrappedType.isEnum()) {
            return Enum.valueOf((Class<Enum>) unwrappedType, str);
        }

        if (Number.class.isAssignableFrom(unwrappedType)) {
            if (unwrappedType == Long.class) {
                if (value instanceof Number n) return n.longValue();
                if (value instanceof java.util.Date d) return d.getTime();
                if (value instanceof Instant inst) return inst.toEpochMilli();
                if (value instanceof LocalDateTime ldt) return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (value instanceof LocalDate ld) return ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (str.contains("T") || str.contains("-")) {
                    try {
                        Object dt = convertToDateTime(value, Instant.class);
                        if (dt instanceof Instant inst) return inst.toEpochMilli();
                    } catch (Exception ignored) {}
                }
                return Long.parseLong(str.replaceAll("[^0-9.-]", ""));
            }
            if (unwrappedType == Integer.class) {
                if (value instanceof Number n) return n.intValue();
                return Integer.parseInt(str.replaceAll("[^0-9.-]", ""));
            }
            if (unwrappedType == Double.class) {
                if (value instanceof Number n) return n.doubleValue();
                return Double.parseDouble(str);
            }
            if (unwrappedType == Float.class) {
                if (value instanceof Number n) return n.floatValue();
                return Float.parseFloat(str);
            }
            if (unwrappedType == BigDecimal.class) {
                if (value instanceof BigDecimal bd) return bd;
                if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
                return new BigDecimal(str);
            }
            if (unwrappedType == BigInteger.class) {
                if (value instanceof BigInteger bi) return bi;
                return new BigInteger(str);
            }
            if (unwrappedType == Short.class) {
                if (value instanceof Number n) return n.shortValue();
                return Short.parseShort(str);
            }
            if (unwrappedType == Byte.class) {
                if (value instanceof Number n) return n.byteValue();
                return Byte.parseByte(str);
            }
        }

        if (unwrappedType == Boolean.class) {
            if (value instanceof Boolean b) return b;
            return "true".equalsIgnoreCase(str) || "1".equals(str) || "yes".equalsIgnoreCase(str);
        }

        if (unwrappedType == UUID.class) {
            return UUID.fromString(str);
        }

        if (isDateTimeType(unwrappedType)) {
            return convertToDateTime(value, unwrappedType);
        }

        return value;
    }

    private boolean isDateTimeType(Class<?> type) {
        if (type == null) return false;
        return Temporal.class.isAssignableFrom(type)
                || java.util.Date.class.isAssignableFrom(type)
                || type == java.sql.Date.class
                || type == java.sql.Timestamp.class;
    }

    private Class<?> wrapPrimitive(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private Object convertToDateTime(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        LocalDateTime ldt = null;
        Instant instant = null;
        LocalDate ld = null;

        if (value instanceof LocalDateTime v) {
            ldt = v;
            instant = v.atZone(ZoneId.systemDefault()).toInstant();
            ld = v.toLocalDate();
        } else if (value instanceof LocalDate v) {
            ld = v;
            ldt = v.atStartOfDay();
            instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
        } else if (value instanceof Instant v) {
            instant = v;
            ldt = LocalDateTime.ofInstant(v, ZoneId.systemDefault());
            ld = ldt.toLocalDate();
        } else if (value instanceof java.util.Date v) {
            instant = v.toInstant();
            ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            ld = ldt.toLocalDate();
        } else if (value instanceof Number num) {
            instant = Instant.ofEpochMilli(num.longValue());
            ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            ld = ldt.toLocalDate();
        } else {
            String str = value.toString().trim();
            if (str.isEmpty()) return null;

            try {
                if (str.endsWith("Z")) {
                    instant = Instant.parse(str);
                    ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    ld = ldt.toLocalDate();
                } else if (str.contains("+") || (str.lastIndexOf('-') > 10)) {
                    OffsetDateTime odt = OffsetDateTime.parse(str);
                    instant = odt.toInstant();
                    ldt = odt.toLocalDateTime();
                    ld = ldt.toLocalDate();
                } else if (str.contains("T")) {
                    ldt = LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
                    ld = ldt.toLocalDate();
                } else if (str.contains(" ") && str.length() > 10) {
                    ldt = LocalDateTime.parse(str.replace(" ", "T"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
                    ld = ldt.toLocalDate();
                } else if (str.length() == 10 && str.charAt(4) == '-' && str.charAt(7) == '-') {
                    ld = LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
                    ldt = ld.atStartOfDay();
                    instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
                } else {
                    try {
                        ldt = LocalDateTime.parse(str);
                        instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
                        ld = ldt.toLocalDate();
                    } catch (Exception e1) {
                        instant = Instant.parse(str);
                        ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                        ld = ldt.toLocalDate();
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot parse date/time string '" + str + "': " + e.getMessage(), e);
            }
        }

        if (targetType == LocalDateTime.class) {
            return ldt;
        }
        if (targetType == LocalDate.class) {
            return ld;
        }
        if (targetType == LocalTime.class) {
            return ldt != null ? ldt.toLocalTime() : null;
        }
        if (targetType == Instant.class) {
            return instant;
        }
        if (targetType == OffsetDateTime.class) {
            return instant != null ? instant.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
        }
        if (targetType == ZonedDateTime.class) {
            return instant != null ? instant.atZone(ZoneId.systemDefault()) : null;
        }
        if (targetType == java.sql.Timestamp.class) {
            return instant != null ? java.sql.Timestamp.from(instant) : null;
        }
        if (targetType == java.sql.Date.class) {
            return ld != null ? java.sql.Date.valueOf(ld) : null;
        }
        if (java.util.Date.class.isAssignableFrom(targetType)) {
            return instant != null ? java.util.Date.from(instant) : null;
        }

        return ldt;
    }

    private Field findDeclaredField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private EntityMetadata requireMetadata(String entityName) {
        EntityMetadata meta = scanner.getMetadata(entityName);
        if (meta == null) {
            throw new NoSuchElementException("Entity " + entityName + " not found or not registered with JPA Admin");
        }
        return meta;
    }

    private String resolveEntityLabel(Object entity, EntityMetadata metadata, String lookupProp) {
        if (entity == null) return "";

        // 1. Explicit lookup property if configured
        if (lookupProp != null && !lookupProp.trim().isEmpty()) {
            Object val = getFieldValue(entity, lookupProp.trim());
            if (val != null) {
                String str = val.toString().trim();
                if (!str.isEmpty()) return str;
            }
        }

        // 2. Prioritize entity's own toString() if overridden
        try {
            Method toStringMethod = entity.getClass().getMethod("toString");
            if (toStringMethod.getDeclaringClass() != Object.class) {
                String str = entity.toString();
                if (str != null && !str.trim().isEmpty() && !isDefaultToString(entity, str.trim())) {
                    return str.trim();
                }
            }
        } catch (Exception ignored) {}

        // 3. Common naming properties
        String[] candidateProps = {"name", "title", "label", "fullName", "displayName", "username", "email", "code", "description"};
        for (String prop : candidateProps) {
            Object val = getFieldValue(entity, prop);
            if (val != null) {
                String str = val.toString().trim();
                if (!str.isEmpty()) return str;
            }
        }

        // 4. Any non-primary key, non-sensitive string field from metadata
        if (metadata != null) {
            for (FieldMetadata f : metadata.getFields()) {
                if (!f.isPrimaryKey() && f.getFieldType() == FieldType.STRING && !f.isSensitive()) {
                    Object val = getFieldValue(entity, f.getName());
                    if (val != null) {
                        String str = val.toString().trim();
                        if (!str.isEmpty()) return str;
                    }
                }
            }
        }

        // 5. Fallback to Primary Key
        String pkField = (metadata != null) ? metadata.getPrimaryKeyField() : "id";
        Object idVal = getFieldValue(entity, pkField);
        return (idVal != null) ? String.valueOf(idVal) : "";
    }

    private boolean isDefaultToString(Object obj, String str) {
        if (str == null || str.isEmpty()) return true;
        String fullClassName = obj.getClass().getName();
        String simpleClassName = obj.getClass().getSimpleName();
        if (str.startsWith(fullClassName + "@") || str.startsWith(simpleClassName + "@")) {
            return true;
        }
        Class<?> superclass = obj.getClass().getSuperclass();
        if (superclass != null && superclass != Object.class) {
            if (str.startsWith(superclass.getName() + "@") || str.startsWith(superclass.getSimpleName() + "@")) {
                return true;
            }
        }
        return false;
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load entity class: " + className, e);
        }
    }
}
