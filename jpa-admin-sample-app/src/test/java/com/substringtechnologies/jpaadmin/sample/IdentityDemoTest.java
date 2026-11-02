package com.substringtechnologies.jpaadmin.sample;

import com.substringtechnologies.jpaadmin.security.AdminIdentityStore;
import com.substringtechnologies.jpaadmin.sample.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:identitydemo-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.admin.security.username=admin", "spring.jpa.admin.security.password=admin-demo-123"})
@ActiveProfiles("identity-demo")
class IdentityDemoTest {
    @Autowired AdminIdentityStore store;
    @Test void profileCreatesSeparateRestrictedAccountsAndSuperuser() {
        var root = store.findByUsername("admin").orElseThrow();
        var viewer = store.findByUsername("viewer").orElseThrow();
        var editor = store.findByUsername("editor").orElseThrow();
        assertTrue(root.superuser());
        assertFalse(viewer.superuser());
        assertEquals(java.util.Set.of(Product.class.getName() + ":view"), viewer.permissions());
        assertTrue(editor.permissions().contains(Product.class.getName() + ":change"));
        assertFalse(editor.permissions().contains(Product.class.getName() + ":delete"));
        var encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches("viewer-demo-123", viewer.passwordHash()));
        assertTrue(encoder.matches("editor-demo-123", editor.passwordHash()));
    }
}
