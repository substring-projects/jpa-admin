package com.substringtechnologies.jpaadmin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.substringtechnologies.jpaadmin.metadata.*;
import java.util.*;

/** One policy for service enforcement, the permission catalog and user-specific UI metadata. */
public class AdminAccess {
    private final JpaMetamodelScanner scanner;
    private final AdminAuthorization authorization;
    private final ObjectMapper mapper = new ObjectMapper().configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(com.fasterxml.jackson.databind.MapperFeature.USE_GETTERS_AS_SETTERS, false);
    public AdminAccess(JpaMetamodelScanner scanner, AdminAuthorization authorization) {
        this.scanner = scanner; this.authorization = authorization;
    }
    public String subject() { return authorization.subject(); }
    public boolean can(EntityMetadata entity, String operation) {
        if (entity == null || !authorization.canEnterAdmin()) return false;
        if (!authorization.isAllowed(entity.getPermissionResource(), "view")) return false;
        boolean enabled = switch (operation) {
            case "view" -> true;
            case "add" -> entity.isCreatable() && !entity.isReadOnly();
            case "change" -> entity.isUpdatable() && !entity.isReadOnly();
            case "delete" -> entity.isDeletable() && !entity.isReadOnly();
            default -> operation.startsWith("action:") && !entity.isReadOnly()
                    && entity.getActions().stream().anyMatch(a -> operation.equals("action:" + a.getId()));
        };
        return enabled && authorization.isAllowed(entity.getPermissionResource(), operation);
    }
    public boolean canView(String entity) { return can(scanner.getMetadata(entity), "view"); }
    public void require(EntityMetadata entity, String operation) {
        if (!can(entity, operation)) throw new AdminAccessDeniedException();
    }
    public void requireView(String entity) { require(scanner.getMetadata(entity), "view"); }
    public List<Map<String, String>> catalog() {
        var result = new ArrayList<Map<String, String>>();
        for (var entity : scanner.getAllMetadata()) {
            for (String op : List.of("view", "add", "change", "delete")) {
                result.add(Map.of("key", entity.getPermissionResource() + ":" + op,
                        "entity", entity.getDisplayName(), "operation", op));
            }
            for (var action : entity.getActions()) result.add(Map.of("key", entity.getPermissionResource() + ":action:" + action.getId(),
                    "entity", entity.getDisplayName(), "operation", "action:" + action.getId()));
        }
        return result;
    }
    public Collection<EntityMetadata> metadata() {
        return scanner.getAllMetadata().stream().filter(e -> can(e, "view")).map(entity -> {
            // Never write request-specific permissions into the scanner's shared metadata.
            EntityMetadata copy = mapper.convertValue(entity, EntityMetadata.class);
            var permissions = new LinkedHashMap<String, Boolean>();
            for (String op : List.of("view", "add", "change", "delete")) permissions.put(op, can(entity, op));
            copy.setPermissions(permissions);
            copy.setCreatable(can(entity, "add")); copy.setUpdatable(can(entity, "change")); copy.setDeletable(can(entity, "delete"));
            copy.setActions(copy.getActions().stream().filter(a -> can(entity, "action:" + a.getId())).toList());
            copy.setFields(copy.getFields().stream().filter(f -> f.getLookupTargetEntity() == null
                    || f.getLookupTargetEntity().isBlank() || canView(f.getLookupTargetEntity())).toList());
            copy.setInlines(copy.getInlines().stream().filter(i -> canView(i.getTargetEntity())).peek(i -> {
                var child = scanner.getMetadata(i.getTargetEntity());
                i.setFields(i.getFields().stream().filter(f -> f.getLookupTargetEntity() == null
                        || f.getLookupTargetEntity().isBlank() || canView(f.getLookupTargetEntity())).toList());
                i.setCanAdd(i.isCanAdd() && can(child, "add"));
                i.setCanDelete(i.isCanDelete() && can(child, "delete"));
                // A read-only child collection must never be posted back by the parent form.
                if (!can(child, "change")) i.setMappedBy(null);
            }).toList());
            return copy;
        }).toList();
    }
}
