package com.substringtechnologies.jpaadmin.metadata;

import com.substringtechnologies.jpaadmin.annotations.*;
import com.substringtechnologies.jpaadmin.config.AdminRegistry;
import com.substringtechnologies.jpaadmin.config.JpaAdminConfigurer;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JpaMetamodelScanner {

    static final Set<String> CODE_LANGUAGES = Set.of("json", "javascript", "typescript", "html", "css", "sql", "xml", "markdown", "yaml", "python", "text");

    private final EntityManager entityManager;
    private final List<JpaAdminConfigurer> configurers;
    private final Map<String, EntityMetadata> entityRegistry = new ConcurrentHashMap<>();
    private int defaultPageSize = 25;

    public JpaMetamodelScanner(EntityManager entityManager) {
        this(entityManager, List.of());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JpaMetamodelScanner(EntityManager entityManager, List<JpaAdminConfigurer> configurers) {
        this.entityManager = entityManager;
        this.configurers = List.copyOf(configurers);
    }

    /** Rows per page for entities whose {@code @AdminResource} doesn't set {@code pageSize}. */
    public void setDefaultPageSize(int defaultPageSize) {
        if (defaultPageSize < 1 || defaultPageSize > 500) throw new IllegalArgumentException("spring.jpa.admin.page-size must be between 1 and 500");
        this.defaultPageSize = defaultPageSize;
    }

    @PostConstruct
    public void scan() {
        Metamodel metamodel = entityManager.getMetamodel();
        java.util.Set<String> permissionResources = new java.util.HashSet<>();
        for (EntityType<?> entityType : metamodel.getEntities()) {
            Class<?> javaType = entityType.getJavaType();
            if (javaType == null) continue;

            AdminResource resourceAnn = javaType.getAnnotation(AdminResource.class);
            if (resourceAnn != null && resourceAnn.hidden()) {
                continue;
            }

            EntityMetadata metadata = new EntityMetadata();
            metadata.setEntityName(entityType.getName());
            metadata.setJavaClassName(javaType.getName());

            if (resourceAnn != null) {
                if (hasText(resourceAnn.permissionResource())) metadata.setPermissionResource(resourceAnn.permissionResource());
                if (hasText(resourceAnn.permissionResource()) && !resourceAnn.permissionResource().matches("[A-Za-z0-9_.-]+")) throw new IllegalStateException("Invalid permission resource: " + metadata.getPermissionResource());
                metadata.setDisplayName(hasText(resourceAnn.displayName()) ? resourceAnn.displayName() : humanize(entityType.getName()));
                metadata.setPluralName(hasText(resourceAnn.pluralName()) ? resourceAnn.pluralName() : metadata.getDisplayName() + "s");
                metadata.setGroup(resourceAnn.group());
                metadata.setIcon(resourceAnn.icon());
                metadata.setOrder(resourceAnn.order());
                metadata.setReadOnly(resourceAnn.readOnly());
                metadata.setCreatable(resourceAnn.creatable());
                metadata.setUpdatable(resourceAnn.updatable());
                metadata.setDeletable(resourceAnn.deletable());
                metadata.setExportable(resourceAnn.exportable());
                metadata.setDefaultSortBy(resourceAnn.defaultSortBy());
                metadata.setDefaultSortDir(resourceAnn.defaultSortDir());
                if (resourceAnn.pageSize() < 0 || resourceAnn.pageSize() > 500) throw new IllegalStateException("@AdminResource pageSize on " + javaType.getSimpleName() + " must be between 1 and 500");
                metadata.setPageSize(resourceAnn.pageSize() > 0 ? resourceAnn.pageSize() : defaultPageSize);
            } else {
                metadata.setPageSize(defaultPageSize);
                metadata.setDisplayName(humanize(entityType.getName()));
                metadata.setPluralName(metadata.getDisplayName() + "s");
                metadata.setGroup("General");
                metadata.setIcon("database");
                metadata.setOrder(100);
            }

            if (!permissionResources.add(metadata.getPermissionResource())) throw new IllegalStateException("Duplicate permission resource: " + metadata.getPermissionResource());

            // Inspect Attributes
            for (Attribute<?, ?> attr : entityType.getAttributes()) {
                Field field = getDeclaredField(javaType, attr.getName());
                FieldMetadata fieldMeta = inspectField(attr, field, javaType);
                if (fieldMeta != null) {
                    metadata.getFields().add(fieldMeta);
                    if (fieldMeta.isPrimaryKey()) {
                        metadata.setPrimaryKeyField(fieldMeta.getName());
                    }
                }
            }

            // Inspect Inlines (@AdminInline on @OneToMany / @ManyToMany)
            inspectInlines(metadata, javaType);

            // Inspect Virtual Computed methods (@AdminComputed)
            inspectComputedMethods(metadata, javaType);

            // Inspect Actions (@AdminAction)
            inspectActions(metadata, javaType);

            // Sort fields by order
            metadata.getFields().sort(Comparator.comparingInt(FieldMetadata::getOrder));

            entityRegistry.put(entityType.getName().toLowerCase(), metadata);
        }
        validateLookups();
        applyConfigurers();
    }

    /** Lookup properties name text fields of the related entity; a typo fails startup instead of every picker. */
    private void validateLookups() {
        for (EntityMetadata entity : entityRegistry.values()) {
            for (FieldMetadata field : entity.getFields()) {
                if (field.getLookupTargetEntity() == null) continue;
                EntityMetadata target = getMetadata(field.getLookupTargetEntity());
                String where = "@AdminLookup on " + entity.getEntityName() + "." + field.getName();
                for (String property : new String[]{field.getLookupProperty(), field.getSearchProperty()}) {
                    if (!hasText(property)) continue;
                    if (target == null) throw new IllegalStateException(where + " targets " + field.getLookupTargetEntity() + ", which isn't shown in JPA Admin");
                    FieldMetadata candidate = target.getField(property);
                    if (candidate == null || candidate.isSensitive() || candidate.isComputed())
                        throw new IllegalStateException(where + " names '" + property + "', which isn't a visible field of " + target.getEntityName());
                }
                if (hasText(field.getSearchProperty()) && target.getField(field.getSearchProperty()).getFieldType() != FieldType.STRING)
                    throw new IllegalStateException(where + " searchProperty '" + field.getSearchProperty() + "' must be a text field");
            }
        }
    }

    private void applyConfigurers() {
        if (configurers.isEmpty()) return;
        AdminRegistry registry = new AdminRegistry();
        configurers.forEach(configurer -> configurer.configure(registry));
        registry.getCustomizers().forEach((type, customizer) -> {
            EntityMetadata metadata = entityRegistry.values().stream()
                    .filter(entity -> entity.getJavaClassName().equals(type.getName())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("JpaAdminConfigurer customizes " + type.getName() + ", which isn't a JPA entity shown in JPA Admin"));
            if (hasText(customizer.getDisplayName())) {
                metadata.setDisplayName(customizer.getDisplayName());
                metadata.setPluralName(customizer.getDisplayName() + "s");
            }
            if (hasText(customizer.getPluralName())) metadata.setPluralName(customizer.getPluralName());
            if (hasText(customizer.getIcon())) metadata.setIcon(customizer.getIcon());
            if (hasText(customizer.getGroup())) metadata.setGroup(customizer.getGroup());
            if (customizer.getOrder() != null) metadata.setOrder(customizer.getOrder());
        });
    }

    private FieldMetadata inspectField(Attribute<?, ?> attr, Field field, Class<?> entityClass) {
        FieldMetadata meta = new FieldMetadata();
        meta.setName(attr.getName());

        Class<?> fieldJavaType = attr.getJavaType();
        meta.setJavaType(fieldJavaType != null ? fieldJavaType.getSimpleName() : "Object");

        AdminField adminField = (field != null) ? field.getAnnotation(AdminField.class) : null;
        if (adminField != null) {
            meta.setLabel(hasText(adminField.label()) ? adminField.label() : humanize(attr.getName()));
            meta.setDescription(adminField.description());
            meta.setOrder(adminField.order());
            meta.setInList(adminField.inList());
            meta.setInForm(adminField.inForm());
            meta.setInDetail(adminField.inDetail());
            meta.setReadOnly(adminField.readOnly());
            meta.setSearchable(adminField.searchable());
            meta.setSortable(adminField.sortable());
            meta.setFilterable(adminField.filterable());
            meta.setPlaceholder(adminField.placeholder());
        } else {
            meta.setLabel(humanize(attr.getName()));
            meta.setOrder(100);
        }

        // Auto-detect read-only creation fields & non-updatable columns
        String lowerName = attr.getName().toLowerCase().replace("_", "").replace("-", "");
        if (lowerName.equals("createdat") || lowerName.equals("createddate")
                || lowerName.equals("createdon") || lowerName.equals("creationdate")
                || lowerName.equals("createtime") || lowerName.equals("creationtime")
                || lowerName.equals("createddatetime")) {
            meta.setReadOnly(true);
            meta.setInForm(false);
        }

        if (field != null) {
            for (java.lang.annotation.Annotation ann : field.getAnnotations()) {
                String annName = ann.annotationType().getSimpleName();
                if ("CreatedDate".equals(annName) || "CreationTimestamp".equals(annName)) {
                    meta.setReadOnly(true);
                    meta.setInForm(false);
                }
            }
            if (field.isAnnotationPresent(Column.class)) {
                Column col = field.getAnnotation(Column.class);
                if (!col.updatable()) {
                    meta.setReadOnly(true);
                }
            }
        }

        // Widget
        AdminWidget adminWidget = (field != null) ? field.getAnnotation(AdminWidget.class) : null;
        if (adminWidget != null && adminWidget.type() != WidgetType.AUTO) {
            meta.setWidget(adminWidget.type());
        }

        AdminUpload upload = (field != null) ? field.getAnnotation(AdminUpload.class) : null;
        if (upload != null) {
            if (field.getType() != String.class) {
                throw new IllegalStateException("@AdminUpload on " + field.getDeclaringClass().getSimpleName() + "." + field.getName() + " requires a String field that stores the file URL");
            }
            meta.setUpload(new UploadMetadata(upload.storage(), upload.folder().replaceAll("^/+|/+$", ""),
                    List.of(upload.accept()), UploadMetadata.parseSize(upload.maxSize())));
            meta.setWidget(WidgetType.FILE_UPLOAD);
            meta.setFilterable(false);
        }

        // Sensitive
        AdminSensitive sensitive = (field != null) ? field.getAnnotation(AdminSensitive.class) : null;
        if (sensitive != null || attr.getName().toLowerCase().contains("password") || attr.getName().toLowerCase().contains("secret")) {
            meta.setSensitive(true);
            meta.setInList(false); // sensitive hidden from list by default
            meta.setSortable(false);
            meta.setFilterable(false);
            meta.setSearchable(false);
            if (sensitive != null) meta.setMaskPattern(sensitive.mask() && !sensitive.maskPattern().isEmpty() ? sensitive.maskPattern() : null);
            if (meta.getWidget() == WidgetType.AUTO) {
                meta.setWidget(WidgetType.PASSWORD);
            }
        }

        // Detect Primary Key & Generated Value
        boolean isId = (field != null && field.isAnnotationPresent(Id.class)) || attr.getName().equalsIgnoreCase("id");
        meta.setPrimaryKey(isId);
        if (isId) {
            meta.setOrder(1);
            meta.setSortable(true);
            if (field != null && field.isAnnotationPresent(GeneratedValue.class)) {
                meta.setGeneratedValue(true);
                meta.setReadOnly(true);
            }
        }

        // Infer FieldType
        inferTypeAndDefaults(attr, field, meta, fieldJavaType);

        // Validation constraints
        inspectValidation(field, meta);
        applyWidgetOptions(field, meta);

        // Badge Mapping
        AdminBadge badgeAnn = (field != null) ? field.getAnnotation(AdminBadge.class) : null;
        if (badgeAnn != null && badgeAnn.mapping().length > 0) {
            for (BadgeMap map : badgeAnn.mapping()) {
                meta.getBadges().add(new BadgeMetadata(map.value(), map.color(), map.label()));
            }
        }

        return meta;
    }

    private void inferTypeAndDefaults(Attribute<?, ?> attr, Field field, FieldMetadata meta, Class<?> javaType) {
        if (javaType == null) {
            meta.setFieldType(FieldType.UNKNOWN);
            return;
        }

        if (field != null && (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class))) {
            meta.setFieldType(field.isAnnotationPresent(ManyToOne.class) ? FieldType.RELATION_MANY_TO_ONE : FieldType.RELATION_ONE_TO_ONE);
            meta.setLookupTargetEntity(javaType.getSimpleName());
            AdminLookup lookup = field.getAnnotation(AdminLookup.class);
            if (lookup != null) {
                meta.setLookupProperty(lookup.lookupProperty());
                meta.setSearchProperty(lookup.searchProperty());
            }
            return;
        }

        if (field != null && (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class))) {
            boolean manyToMany = field.isAnnotationPresent(ManyToMany.class);
            meta.setFieldType(manyToMany ? FieldType.RELATION_MANY_TO_MANY : FieldType.RELATION_ONE_TO_MANY);
            meta.setInList(false);
            Class<?> elementType = getGenericType(field);
            if (elementType != null) meta.setLookupTargetEntity(elementType.getSimpleName());
            // Only the owning side of a many-to-many writes the join table, so the inverse side stays read-only.
            boolean owning = manyToMany && field.getAnnotation(ManyToMany.class).mappedBy().isEmpty();
            meta.setInForm(owning && field.getAnnotation(AdminInline.class) == null);
            if (!owning) meta.setReadOnly(true);
            AdminLookup lookup = field.getAnnotation(AdminLookup.class);
            if (lookup != null) {
                meta.setLookupProperty(lookup.lookupProperty());
                meta.setSearchProperty(lookup.searchProperty());
            }
            return;
        }

        if (field != null && field.isAnnotationPresent(Embedded.class)) {
            meta.setFieldType(FieldType.EMBEDDED);
            return;
        }

        if (javaType.isEnum()) {
            meta.setFieldType(FieldType.ENUM);
            meta.setFilterable(true);
            List<String> enumConstants = Arrays.stream(javaType.getEnumConstants()).map(Object::toString).toList();
            meta.setEnumValues(enumConstants);
            return;
        }

        if (String.class.isAssignableFrom(javaType)) {
            meta.setFieldType(FieldType.STRING);
            if (field != null && field.isAnnotationPresent(Lob.class)) {
                if (meta.getWidget() == WidgetType.AUTO) {
                    meta.setWidget(WidgetType.TEXTAREA);
                }
            }
        } else if (Number.class.isAssignableFrom(javaType) || javaType.isPrimitive() &&
                (javaType == int.class || javaType == long.class || javaType == double.class || javaType == float.class || javaType == short.class)) {
            meta.setFieldType(FieldType.NUMBER);
        } else if (Boolean.class.isAssignableFrom(javaType) || javaType == boolean.class) {
            meta.setFieldType(FieldType.BOOLEAN);
            meta.setFilterable(true);
            if (meta.getWidget() == WidgetType.AUTO) {
                meta.setWidget(WidgetType.BOOLEAN_SWITCH);
            }
        } else if (LocalDate.class.isAssignableFrom(javaType)) {
            meta.setFieldType(FieldType.DATE);
            meta.setFilterable(true);
        } else if (LocalDateTime.class.isAssignableFrom(javaType) ||
                   Date.class.isAssignableFrom(javaType) ||
                   java.time.temporal.Temporal.class.isAssignableFrom(javaType)) {
            meta.setFieldType(FieldType.DATE_TIME);
            meta.setFilterable(true);
        } else {
            meta.setFieldType(FieldType.STRING);
        }
    }

    void applyWidgetOptions(Field field, FieldMetadata meta) {
        AdminWidget widget = field == null ? null : field.getAnnotation(AdminWidget.class);
        if (widget == null) return;
        String where = "@AdminWidget on " + field.getDeclaringClass().getSimpleName() + "." + field.getName();
        boolean hasMin = Double.isFinite(widget.min()), hasMax = Double.isFinite(widget.max());
        if (hasMin || hasMax || widget.step() != 0 || widget.type() == WidgetType.SLIDER) {
            if (meta.getFieldType() != FieldType.NUMBER) throw new IllegalStateException(where + ": min, max, step and SLIDER require a numeric field");
            if (hasMin && hasMax && widget.min() > widget.max()) throw new IllegalStateException(where + ": min must not exceed max");
            if (!(widget.step() >= 0) || Double.isInfinite(widget.step())) throw new IllegalStateException(where + ": step must be 0 or a positive number");
            if (widget.type() == WidgetType.SLIDER && !(hasMin && hasMax)) throw new IllegalStateException(where + ": SLIDER needs both min and max");
            if (hasMin) meta.setMin(widget.min());
            if (hasMax) meta.setMax(widget.max());
            if (widget.step() > 0) meta.setStep(widget.step());
        }
        if (meta.getWidget() == WidgetType.CODE_EDITOR) {
            String language = widget.language().trim().toLowerCase(Locale.ROOT);
            if (!CODE_LANGUAGES.contains(language))
                throw new IllegalStateException(where + ": unsupported language '" + widget.language() + "'. Use one of " + new TreeSet<>(CODE_LANGUAGES));
            meta.setLanguage(language);
        }
        if (hasText(widget.previewSize())) {
            String size = widget.previewSize().trim();
            if (!size.matches("\\d{1,3}(\\.\\d+)?(px|rem|em)")) throw new IllegalStateException(where + ": previewSize must be a CSS length such as 64px or 4rem");
            meta.setPreviewSize(size);
        }
    }

    private void inspectValidation(Field field, FieldMetadata meta) {
        if (field == null) return;
        ValidationMetadata v = meta.getValidation();

        if (field.isAnnotationPresent(NotNull.class) || field.isAnnotationPresent(NotBlank.class) || field.isAnnotationPresent(NotEmpty.class)) {
            v.setRequired(true);
        }
        if (field.isAnnotationPresent(Column.class)) {
            Column col = field.getAnnotation(Column.class);
            if (!col.nullable() && !meta.isGeneratedValue()) {
                v.setRequired(true);
            }
            if (col.length() > 0 && col.length() != 255) {
                v.setMaxLength(col.length());
            }
        }
        if (field.isAnnotationPresent(Size.class)) {
            Size size = field.getAnnotation(Size.class);
            v.setMinLength(size.min());
            v.setMaxLength(size.max());
        }
        if (field.isAnnotationPresent(Min.class)) {
            v.setMin((double) field.getAnnotation(Min.class).value());
        }
        if (field.isAnnotationPresent(Max.class)) {
            v.setMax((double) field.getAnnotation(Max.class).value());
        }
        if (field.isAnnotationPresent(Positive.class)) {
            v.setMin(0.01);
        }
        if (field.isAnnotationPresent(Email.class)) {
            v.setEmail(true);
        }
        if (field.isAnnotationPresent(Pattern.class)) {
            v.setPattern(field.getAnnotation(Pattern.class).regexp());
        }
    }

    private void inspectInlines(EntityMetadata metadata, Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            AdminInline inlineAnn = field.getAnnotation(AdminInline.class);
            if (inlineAnn != null && (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class))) {
                InlineMetadata inline = new InlineMetadata();
                inline.setPropertyName(field.getName());
                inline.setType(inlineAnn.type());
                inline.setTitle(hasText(inlineAnn.title()) ? inlineAnn.title() : humanize(field.getName()));
                inline.setCanAdd(inlineAnn.canAdd());
                inline.setCanDelete(inlineAnn.canDelete());
                inline.setLimit(inlineAnn.limit());

                if (field.isAnnotationPresent(OneToMany.class)) {
                    inline.setMappedBy(field.getAnnotation(OneToMany.class).mappedBy());
                }

                // Determine target entity type
                Class<?> targetClass = getGenericType(field);
                if (targetClass != null) {
                    inline.setTargetEntity(targetClass.getSimpleName());
                }
                metadata.getInlines().add(inline);
            }
        }
    }

    private void inspectComputedMethods(EntityMetadata metadata, Class<?> entityClass) {
        for (Method method : entityClass.getDeclaredMethods()) {
            AdminComputed computed = method.getAnnotation(AdminComputed.class);
            if (computed != null && method.getParameterCount() == 0 && (method.getName().startsWith("get") || method.getName().startsWith("is"))) {
                FieldMetadata meta = new FieldMetadata();
                String propName = Character.toLowerCase(method.getName().replaceAll("^(get|is)", "").charAt(0))
                        + method.getName().replaceAll("^(get|is)", "").substring(1);
                meta.setName(propName);
                meta.setLabel(hasText(computed.label()) ? computed.label() : humanize(propName));
                meta.setOrder(computed.order());
                meta.setInList(computed.inList());
                meta.setInDetail(computed.inDetail());
                meta.setInForm(false); // Virtual fields cannot be directly edited in form
                meta.setReadOnly(true);
                meta.setComputed(true);
                meta.setFormat(computed.format());
                meta.setJavaType(method.getReturnType().getSimpleName());
                meta.setFieldType(FieldType.STRING);
                metadata.getFields().add(meta);
            }
        }
    }

    private void inspectActions(EntityMetadata metadata, Class<?> entityClass) {
        for (Method method : entityClass.getDeclaredMethods()) {
            AdminAction action = method.getAnnotation(AdminAction.class);
            if (action != null) {
                ActionMetadata actMeta = new ActionMetadata();
                actMeta.setId(method.getName());
                actMeta.setName(action.name());
                actMeta.setIcon(action.icon());
                actMeta.setConfirmation(action.confirmation());
                actMeta.setScope(action.scope());
                actMeta.setMethodName(method.getName());
                actMeta.setStatic(Modifier.isStatic(method.getModifiers()));
                metadata.getActions().add(actMeta);
            }
        }
    }

    public EntityMetadata getMetadata(String entityName) {
        return entityRegistry.get(entityName.toLowerCase());
    }

    public Collection<EntityMetadata> getAllMetadata() {
        return entityRegistry.values();
    }

    private Field getDeclaredField(Class<?> clazz, String fieldName) {
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

    private Class<?> getGenericType(Field field) {
        try {
            java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) field.getGenericType();
            return (Class<?>) pt.getActualTypeArguments()[0];
        } catch (Exception e) {
            return null;
        }
    }

    private String humanize(String text) {
        if (text == null || text.isEmpty()) return "";
        String spaced = text.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
