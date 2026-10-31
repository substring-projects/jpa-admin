package com.substringtechnologies.jpaadmin.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.substringtechnologies.jpaadmin.boot.config.JpaAdminProperties;
import com.substringtechnologies.jpaadmin.boot.config.JpaAdminWebMvcConfigurer;
import com.substringtechnologies.jpaadmin.metadata.JpaMetamodelScanner;
import com.substringtechnologies.jpaadmin.service.DynamicCrudService;
import com.substringtechnologies.jpaadmin.storage.FileStorage;
import com.substringtechnologies.jpaadmin.storage.StorageRegistry;
import com.substringtechnologies.jpaadmin.storage.UploadService;
import com.substringtechnologies.jpaadmin.storage.UploadTokens;
import com.substringtechnologies.jpaadmin.web.AdminApiController;
import com.substringtechnologies.jpaadmin.web.AdminUploadController;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
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
    public JpaAdminWebMvcConfigurer jpaAdminWebMvcConfigurer(JpaAdminProperties properties, StorageRegistry storages) {
        return new JpaAdminWebMvcConfigurer(properties, storages);
    }

    @Bean
    @ConditionalOnMissingBean
    public StorageRegistry jpaAdminStorageRegistry(JpaAdminProperties properties, ObjectProvider<FileStorage> customStorages) {
        return new StorageRegistry(properties.getStorage(), properties.getPath() + "/api/uploads/local", customStorages.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public UploadTokens jpaAdminUploadTokens(JpaAdminProperties properties) {
        return new UploadTokens(properties.getUpload().getSigningKey());
    }

    @Bean
    @ConditionalOnMissingBean
    public UploadService jpaAdminUploadService(JpaMetamodelScanner scanner, StorageRegistry storages, UploadTokens tokens) {
        return new UploadService(scanner, storages, tokens);
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminUploadController adminUploadController(UploadService uploads) {
        return new AdminUploadController(uploads);
    }

    /** Fails startup when an @AdminUpload field names a storage that isn't configured, instead of failing on first upload. */
    @Bean
    public SmartInitializingSingleton jpaAdminUploadConfigurationCheck(JpaMetamodelScanner scanner, StorageRegistry storages) {
        return () -> {
            var problems = UploadService.misconfiguredFields(scanner, storages);
            if (!problems.isEmpty()) {
                throw new IllegalStateException("@AdminUpload fields reference storages that aren't configured: " + String.join("; ", problems)
                        + ". Configure spring.jpa.admin.storage.<name> (currently configured: " + storages.names() + ")");
            }
        };
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
