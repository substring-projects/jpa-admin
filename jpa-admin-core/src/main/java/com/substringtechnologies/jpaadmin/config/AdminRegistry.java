package com.substringtechnologies.jpaadmin.config;

import java.util.LinkedHashMap;
import java.util.Map;

/** Entity customizations collected from {@link JpaAdminConfigurer} beans; they override {@code @AdminResource} values. */
public class AdminRegistry {
    private final Map<Class<?>, EntityCustomizer> customizers = new LinkedHashMap<>();

    public EntityCustomizer forEntity(Class<?> entityClass) {
        return customizers.computeIfAbsent(entityClass, EntityCustomizer::new);
    }

    public Map<Class<?>, EntityCustomizer> getCustomizers() {
        return customizers;
    }

    public static class EntityCustomizer {
        private final Class<?> entityClass;
        private String displayName;
        private String pluralName;
        private String icon;
        private String group;
        private Integer order;

        public EntityCustomizer(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        /** Singular name; the plural becomes the name plus "s" unless {@link #pluralName(String)} is set. */
        public EntityCustomizer displayName(String name) {
            this.displayName = name;
            return this;
        }

        public EntityCustomizer pluralName(String name) {
            this.pluralName = name;
            return this;
        }

        /** Lucide icon name, such as {@code "users"}. */
        public EntityCustomizer icon(String icon) {
            this.icon = icon;
            return this;
        }

        public EntityCustomizer group(String group) {
            this.group = group;
            return this;
        }

        /** Position within the sidebar group; lower values come first. */
        public EntityCustomizer order(int order) {
            this.order = order;
            return this;
        }

        public Class<?> getEntityClass() { return entityClass; }
        public String getDisplayName() { return displayName; }
        public String getPluralName() { return pluralName; }
        public String getIcon() { return icon; }
        public String getGroup() { return group; }
        public Integer getOrder() { return order; }
    }
}
