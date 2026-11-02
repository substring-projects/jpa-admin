package com.substringtechnologies.jpaadmin.security;

import com.substringtechnologies.jpaadmin.metadata.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminAccessTest {
    private AdminAccess access(EntityMetadata entity, Set<String> grants) {
        var scanner = new JpaMetamodelScanner(null) {
            @Override public EntityMetadata getMetadata(String name) { return entity; }
            @Override public Collection<EntityMetadata> getAllMetadata() { return List.of(entity); }
        };
        return new AdminAccess(scanner, new AdminAuthorization() {
            public boolean canEnterAdmin() { return true; }
            public boolean isSuperuser() { return grants.contains("*"); }
            public String subject() { return "test"; }
            public boolean isAllowed(String resource, String op) { return isSuperuser() || grants.contains(op); }
        });
    }
    private EntityMetadata entity() {
        var e = new EntityMetadata(); e.setEntityName("Product"); e.setJavaClassName("example.Product"); e.setDisplayName("Product");
        return e;
    }
    @Test void superuserCannotBypassEntityRestrictions() {
        var entity = entity(); var access = access(entity, Set.of("*"));
        entity.setCreatable(false); assertFalse(access.can(entity, "add"));
        entity.setUpdatable(false); assertFalse(access.can(entity, "change"));
        entity.setDeletable(false); assertFalse(access.can(entity, "delete"));
        entity.setReadOnly(true);
        assertTrue(access.can(entity, "view"));
        assertThrows(AdminAccessDeniedException.class, () -> access.require(entity, "change"));
    }
    @Test void writesAlsoRequireViewAndMetadataDoesNotMutateSharedFlags() {
        var entity = entity();
        assertFalse(access(entity, Set.of("change")).can(entity, "change"));
        var copy = access(entity, Set.of("view")).metadata().iterator().next();
        assertFalse(copy.isCreatable()); assertFalse(copy.isUpdatable()); assertFalse(copy.isDeletable());
        assertTrue(entity.isCreatable()); assertTrue(entity.isUpdatable()); assertTrue(entity.isDeletable());
        assertTrue(access(entity, Set.of("*")).metadata().iterator().next().isUpdatable());
    }
}
