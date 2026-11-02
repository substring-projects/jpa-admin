package com.substringtechnologies.jpaadmin.sample.config;

import com.substringtechnologies.jpaadmin.config.JpaAdminConfigurer;
import com.substringtechnologies.jpaadmin.sample.domain.Category;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Code-based customization, useful for entities you can't annotate. Values here override @AdminResource. */
@Configuration(proxyBeanMethods = false)
public class AdminCustomization {
    @Bean
    JpaAdminConfigurer catalogCustomization() {
        return registry -> registry.forEntity(Category.class).icon("folder-tree").order(1);
    }
}
