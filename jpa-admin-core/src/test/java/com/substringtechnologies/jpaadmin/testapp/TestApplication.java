package com.substringtechnologies.jpaadmin.testapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.substringtechnologies.jpaadmin.config.JpaAdminConfigurer;
import com.substringtechnologies.jpaadmin.metadata.JpaMetamodelScanner;
import com.substringtechnologies.jpaadmin.security.AdminAccess;
import com.substringtechnologies.jpaadmin.security.AdminAuthorization;
import com.substringtechnologies.jpaadmin.service.DynamicCrudService;
import com.substringtechnologies.jpaadmin.web.AdminApiController;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;

/** JPA Admin services over an in-memory database, wired by hand the way auto-configuration does. */
@SpringBootApplication
public class TestApplication {
    static final int DEFAULT_PAGE_SIZE = 40;

    @Bean
    JpaMetamodelScanner scanner(EntityManager entityManager) {
        JpaAdminConfigurer configurer = registry -> registry.forEntity(Team.class).displayName("Squad").icon("users").order(3);
        var scanner = new JpaMetamodelScanner(entityManager, List.of(configurer));
        scanner.setDefaultPageSize(DEFAULT_PAGE_SIZE);
        return scanner;
    }

    @Bean
    AdminAccess access(JpaMetamodelScanner scanner) {
        return new AdminAccess(scanner, new AdminAuthorization() {
            public boolean canEnterAdmin() { return true; }
            public boolean isSuperuser() { return true; }
            public String subject() { return "test"; }
            public boolean isAllowed(String resource, String operation) { return true; }
        });
    }

    @Bean
    DynamicCrudService crud(EntityManager entityManager, JpaMetamodelScanner scanner, ApplicationContext context, AdminAccess access) {
        return new DynamicCrudService(entityManager, scanner, new ObjectMapper(), context, access);
    }

    @Bean
    AdminApiController controller(JpaMetamodelScanner scanner, DynamicCrudService crud, AdminAccess access) {
        return new AdminApiController(scanner, crud, access);
    }
}
