package io.github.jpaadmin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jpaadmin.annotations.ActionScope;
import io.github.jpaadmin.metadata.ActionMetadata;
import io.github.jpaadmin.metadata.EntityMetadata;
import io.github.jpaadmin.metadata.FieldMetadata;
import io.github.jpaadmin.metadata.FieldType;
import io.github.jpaadmin.metadata.InlineMetadata;
import io.github.jpaadmin.metadata.JpaMetamodelScanner;
import jakarta.persistence.EntityManager;
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

        Class<?> entityClass = loadClass(metadata.getJavaClassName());
        try {
            Object entity = entityClass.getDeclaredConstructor().newInstance();
            bindPayload(entity, payload, metadata, false);
            entityManager.persist(entity);
            entityManager.flush();
            return serializeEntity(entity, metadata, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create " + metadata.getDisplayName() + ": " + e.getMessage(), e);
        }
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

        bindPayload(entity, payload, metadata, true);
        entityManager.merge(entity);
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

        // Find primary display property
        for (FieldMetadata f : metadata.getFields()) {
            if (!f.isPrimaryKey() && f.getFieldType() == FieldType.STRING && !f.isSensitive()) {
                displayProp = f.getName();
                break;
            }
        }
        if (displayProp == null) displayProp = pkField;

        if (query != null && !query.trim().isEmpty()) {
            cq.where(cb.like(cb.lower(root.get(displayProp).as(String.class)), "%" + query.toLowerCase() + "%"));
        }

        var typedQuery = entityManager.createQuery(cq);
        typedQuery.setMaxResults(limit > 0 ? limit : 20);

        List<Object> results = typedQuery.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object item : results) {
            Map<String, Object> map = new HashMap<>();
            Object idVal = getFieldValue(item, pkField);
            Object labelVal = getFieldValue(item, displayProp);
            map.put("id", idVal);
            map.put("label", (labelVal != null) ? labelVal.toString() : ("#" + idVal));
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
            filters.forEach((key, val) -> {
                if (metadata.hasField(key) && val != null && !val.trim().isEmpty()) {
                    FieldMetadata field = metadata.getField(key);
                    if (!field.isComputed()) {
                        Object parsedVal = parseFieldValue(val, field, root.getJavaType());
                        if (parsedVal != null) {
                            predicates.add(cb.equal(root.get(key), parsedVal));
                        }
                    }
                }
            });
        }

        return predicates;
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
                    orders.add(metadata.getDefaultSortDir() == io.github.jpaadmin.annotations.SortDir.ASC ?
                            cb.asc(root.get(defaultSort)) : cb.desc(root.get(defaultSort)));
                }
            }
        }
        cq.orderBy(orders);
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

                    String lookupProp = fieldMeta.getLookupProperty();
                    Object labelVal = (lookupProp != null && !lookupProp.isEmpty()) ? getFieldValue(val, lookupProp) : null;
                    if (labelVal == null && relMeta != null) {
                        for (FieldMetadata rf : relMeta.getFields()) {
                            if (!rf.isPrimaryKey() && rf.getFieldType() == FieldType.STRING && !rf.isSensitive()) {
                                labelVal = getFieldValue(val, rf.getName());
                                break;
                            }
                        }
                    }
                    relMap.put("label", (labelVal != null) ? labelVal.toString() : ("#" + relId));
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

    private void bindPayload(Object entity, Map<String, Object> payload, EntityMetadata metadata, boolean isUpdate) {
        for (FieldMetadata fieldMeta : metadata.getFields()) {
            String prop = fieldMeta.getName();
            if (fieldMeta.isPrimaryKey() && fieldMeta.isGeneratedValue()) continue;
            if (fieldMeta.isComputed() || !payload.containsKey(prop)) continue;
            // On update, skip read-only and non-updatable fields (like createdAt)
            if (isUpdate && fieldMeta.isReadOnly()) continue;

            Object rawVal = payload.get(prop);

            // Handle Foreign Keys
            if (fieldMeta.getFieldType() == FieldType.RELATION_MANY_TO_ONE || fieldMeta.getFieldType() == FieldType.RELATION_ONE_TO_ONE) {
                if (rawVal == null || "".equals(rawVal)) {
                    setFieldValue(entity, prop, null);
                } else {
                    Object foreignId = (rawVal instanceof Map<?, ?> m) ? m.get("id") : rawVal;
                    if (foreignId != null && !"".equals(foreignId)) {
                        EntityMetadata relMeta = scanner.getMetadata(fieldMeta.getLookupTargetEntity());
                        Class<?> relClass = loadClass(relMeta.getJavaClassName());
                        Object convertedRelId = convertId(foreignId, relMeta);
                        Object relProxy = entityManager.getReference(relClass, convertedRelId);
                        setFieldValue(entity, prop, relProxy);
                    } else {
                        setFieldValue(entity, prop, null);
                    }
                }
                continue;
            }

            // Normal attribute
            Object parsedVal = parseFieldValue(rawVal, fieldMeta, entity.getClass());
            setFieldValue(entity, prop, parsedVal);
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
            Class<?> targetType = (f != null) ? f.getType() : null;

            Object finalVal = (targetType != null) ? convertValueToTargetType(value, targetType) : value;

            // 1. Try invoking setter method: e.g. setStatus(ProductStatus status)
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            if (targetType != null) {
                try {
                    Method setter = obj.getClass().getMethod(setterName, targetType);
                    setter.invoke(obj, finalVal);
                    return;
                } catch (NoSuchMethodException ignored) {
                } catch (IllegalArgumentException iae) {
                    // Fall back to direct field reflection if setter parameter type mismatch
                }
            }

            // 2. Fallback to direct field reflection
            if (f != null) {
                f.setAccessible(true);
                f.set(obj, finalVal);
            }
        } catch (Exception e) {
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

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load entity class: " + className, e);
        }
    }
}
