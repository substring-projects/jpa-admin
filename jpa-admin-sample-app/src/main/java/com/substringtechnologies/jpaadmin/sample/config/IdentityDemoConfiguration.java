package com.substringtechnologies.jpaadmin.sample.config;

import com.substringtechnologies.jpaadmin.security.AdminIdentityStore;
import com.substringtechnologies.jpaadmin.sample.domain.Category;
import com.substringtechnologies.jpaadmin.sample.domain.Product;
import com.substringtechnologies.jpaadmin.sample.domain.Tag;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

/** Disposable demo accounts, created only when explicitly running the local identity-demo profile. */
@Configuration(proxyBeanMethods = false)
@Profile("identity-demo")
@ConditionalOnProperty(prefix = "spring.jpa.admin.security", name = "mode", havingValue = "database")
public class IdentityDemoConfiguration {
    @Bean
    ApplicationRunner identityDemoAccounts(AdminIdentityStore store) {
        return args -> {
            var readers = group(store, "Product Readers", Set.of(Product.class.getName() + ":view"));
            var editors = group(store, "Catalog Editors", Set.of(Product.class.getName() + ":view",
                    Product.class.getName() + ":add", Product.class.getName() + ":change",
                    Category.class.getName() + ":view", Tag.class.getName() + ":view"));
            var encoder = new BCryptPasswordEncoder();
            if (store.findByUsername("viewer").isEmpty()) store.saveUser(null, "viewer", encoder.encode("viewer-demo-123"),
                    true, true, false, Set.of(readers.id()));
            if (store.findByUsername("editor").isEmpty()) store.saveUser(null, "editor", encoder.encode("editor-demo-123"),
                    true, true, false, Set.of(editors.id()));
        };
    }

    private AdminIdentityStore.Group group(AdminIdentityStore store, String name, Set<String> grants) {
        return store.groups().stream().filter(group -> group.name().equals(name)).findFirst()
                .orElseGet(() -> store.saveGroup(null, name, grants));
    }
}
