package com.substringtechnologies.jpaadmin.config;

/**
 * Customizes entities in code instead of annotations, for example entities from a library you can't annotate.
 * Every bean of this type is applied once at startup, after annotations are read.
 */
@FunctionalInterface
public interface JpaAdminConfigurer {
    void configure(AdminRegistry registry);
}
