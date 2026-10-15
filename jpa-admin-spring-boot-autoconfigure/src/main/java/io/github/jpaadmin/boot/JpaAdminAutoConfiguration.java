package io.github.jpaadmin.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jpaadmin.boot.config.JpaAdminProperties;
import io.github.jpaadmin.boot.config.JpaAdminWebMvcConfigurer;
import io.github.jpaadmin.metadata.JpaMetamodelScanner;
import io.github.jpaadmin.service.DynamicCrudService;
import io.github.jpaadmin.web.AdminApiController;
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

@org.springframework.context.annotation.Import(io.github.jpaadmin.boot.config.JpaAdminSecurityConfiguration.class)
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
