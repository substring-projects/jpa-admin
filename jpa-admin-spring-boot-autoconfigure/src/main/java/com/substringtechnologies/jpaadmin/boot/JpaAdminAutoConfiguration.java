package com.substringtechnologies.jpaadmin.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.substringtechnologies.jpaadmin.boot.config.JpaAdminProperties;
import com.substringtechnologies.jpaadmin.boot.config.JpaAdminWebMvcConfigurer;
import com.substringtechnologies.jpaadmin.metadata.JpaMetamodelScanner;
import com.substringtechnologies.jpaadmin.service.DynamicCrudService;
import com.substringtechnologies.jpaadmin.web.AdminApiController;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@org.springframework.context.annotation.Import(com.substringtechnologies.jpaadmin.boot.config.JpaAdminSecurityConfiguration.class)
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({EntityManagerFactory.class, WebMvcConfigurer.class})
@ConditionalOnProperty(prefix = "spring.jpa.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JpaAdminProperties.class)
public class JpaAdminAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JpaAdminWebMvcConfigurer jpaAdminWebMvcConfigurer(JpaAdminProperties properties) {
        return new JpaAdminWebMvcConfigurer(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JpaMetamodelScanner jpaMetamodelScanner(EntityManager entityManager) {
        return new JpaMetamodelScanner(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicCrudService dynamicCrudService(EntityManager entityManager,
                                                 JpaMetamodelScanner scanner,
                                                 org.springframework.beans.factory.ObjectProvider<ObjectMapper> objectMapperProvider,
                                                 ApplicationContext applicationContext) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new DynamicCrudService(entityManager, scanner, mapper, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminApiController adminApiController(JpaMetamodelScanner scanner, DynamicCrudService dynamicCrudService) {
        return new AdminApiController(scanner, dynamicCrudService);
    }
}
