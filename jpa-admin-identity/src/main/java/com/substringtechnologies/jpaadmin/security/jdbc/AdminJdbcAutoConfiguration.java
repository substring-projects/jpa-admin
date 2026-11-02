package com.substringtechnologies.jpaadmin.security.jdbc;

import com.substringtechnologies.jpaadmin.security.AdminIdentityStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import javax.sql.DataSource;

@AutoConfiguration(beforeName = "com.substringtechnologies.jpaadmin.boot.JpaAdminAutoConfiguration", afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnProperty(prefix = "spring.jpa.admin.security", name = "mode", havingValue = "database")
public class AdminJdbcAutoConfiguration {
    @Bean @ConditionalOnMissingBean(AdminIdentityStore.class)
    @ConditionalOnProperty(prefix = "spring.jpa.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AdminIdentityStore adminIdentityStore(DataSource source, Environment environment) {
        var database = IdentityDatabase.detect(source);
        if (environment.getProperty("spring.jpa.admin.security.initialize-schema", Boolean.class, false)) {
            new ResourceDatabasePopulator(new ClassPathResource(database.migration)).execute(source);
        }
        return new JdbcAdminIdentityStore(source);
    }
}
