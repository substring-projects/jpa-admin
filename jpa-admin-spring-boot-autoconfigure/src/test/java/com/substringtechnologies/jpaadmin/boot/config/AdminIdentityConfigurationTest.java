package com.substringtechnologies.jpaadmin.boot.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminIdentityConfigurationTest {
    @Test void databaseModeNeverFallsBackToConfiguredAccountWithoutStore() {
        var properties = new JpaAdminProperties();
        properties.getSecurity().setMode("database");
        properties.getSecurity().setPassword("configured-password");
        assertThrows(IllegalStateException.class, () -> new AdminIdentityService(properties, null));
    }
    @Test void invalidModeAndMissingConfiguredPasswordFailClosed() {
        var properties = new JpaAdminProperties();
        assertThrows(IllegalArgumentException.class, () -> properties.getSecurity().setMode("typo"));
        assertThrows(IllegalStateException.class, () -> new AdminIdentityService(properties, null));
    }
}
