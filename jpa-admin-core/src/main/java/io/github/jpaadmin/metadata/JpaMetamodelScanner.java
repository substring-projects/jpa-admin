package io.github.jpaadmin.metadata;

import io.github.jpaadmin.annotations.*;
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

    private final EntityManager entityManager;
    private final Map<String, EntityMetadata> entityRegistry = new ConcurrentHashMap<>();

    public JpaMetamodelScanner(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @PostConstruct
    public void scan() {
        Metamodel metamodel = entityManager.getMetamodel();
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
                metadata.setPageSize(resourceAnn.pageSize());
            } else {
                metadata.setDisplayName(humanize(entityType.getName()));
                metadata.setPluralName(metadata.getDisplayName() + "s");
                metadata.setGroup("General");
                metadata.setIcon("database");
                metadata.setOrder(100);
            }

            AdminPermission permAnn = javaType.getAnnotation(AdminPermission.class);
            if (permAnn != null && permAnn.roles().length > 0) {
                metadata.setRoles(Arrays.asList(permAnn.roles()));
            }

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

        // Widget
        AdminWidget adminWidget = (field != null) ? field.getAnnotation(AdminWidget.class) : null;
        if (adminWidget != null && adminWidget.type() != WidgetType.AUTO) {
            meta.setWidget(adminWidget.type());
        }

        // Sensitive
        AdminSensitive sensitive = (field != null) ? field.getAnnotation(AdminSensitive.class) : null;
        if (sensitive != null || attr.getName().toLowerCase().contains("password") || attr.getName().toLowerCase().contains("secret")) {
            meta.setSensitive(true);
            meta.setInList(false); // sensitive hidden from list by default
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
                meta.setQuickCreate(lookup.quickCreate());
            }
            return;
        }

        if (field != null && (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class))) {
            meta.setFieldType(field.isAnnotationPresent(OneToMany.class) ? FieldType.RELATION_ONE_TO_MANY : FieldType.RELATION_MANY_TO_MANY);
            meta.setInList(false);
            meta.setInForm(false);
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
        } else if (LocalDateTime.class.isAssignableFrom(javaType) || Date.class.isAssignableFrom(javaType)) {
            meta.setFieldType(FieldType.DATE_TIME);
            meta.setFilterable(true);
        } else {
            meta.setFieldType(FieldType.STRING);
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
