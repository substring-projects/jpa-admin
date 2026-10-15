package io.github.jpaadmin.config;

import java.util.HashMap;
import java.util.Map;

public class AdminRegistry {
    private final Map<Class<?>, EntityCustomizer> customizers = new HashMap<>();

    public EntityCustomizer forEntity(Class<?> entityClass) {
        return customizers.computeIfAbsent(entityClass, EntityCustomizer::new);
    }

    public Map<Class<?>, EntityCustomizer> getCustomizers() {
        return customizers;
    }

    public static class EntityCustomizer {
        private final Class<?> entityClass;
        private String displayName;
        private String icon;
        private String group;

        public EntityCustomizer(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        public EntityCustomizer displayName(String name) {
            this.displayName = name;
            return this;
        }

        public EntityCustomizer icon(String icon) {
            this.icon = icon;
            return this;
        }

        public EntityCustomizer group(String group) {
            this.group = group;
            return this;
        }

        public Class<?> getEntityClass() { return entityClass; }
        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public String getGroup() { return group; }
    }
}
