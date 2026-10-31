package com.substringtechnologies.jpaadmin.boot.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JpaAdminPropertiesTest {

    @Test
    void acceptsCanonicalPaths() {
        var properties = new JpaAdminProperties();
        properties.setPath("/backoffice");
        assertEquals("/backoffice", properties.getPath());
        properties.setPath("/internal/admin-console");
        assertEquals("/internal/admin-console", properties.getPath());
    }

    @Test
    void rejectsPathsThatWouldDesyncRoutesAndSecurity() {
        var properties = new JpaAdminProperties();
        for (String bad : new String[]{"/", "", "admin", "/admin/", "/ad min", "/admin\"><script>", null}) {
            assertThrows(IllegalArgumentException.class, () -> properties.setPath(bad), String.valueOf(bad));
        }
    }
}
