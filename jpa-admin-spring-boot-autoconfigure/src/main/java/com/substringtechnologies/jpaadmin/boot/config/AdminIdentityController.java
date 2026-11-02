package com.substringtechnologies.jpaadmin.boot.config;

import com.substringtechnologies.jpaadmin.security.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;

@RestController
@RequestMapping("${spring.jpa.admin.path:/admin}/api/security")
public class AdminIdentityController {
    private final AdminIdentityService identity;
    private final AdminAccess access;
    public AdminIdentityController(AdminIdentityService identity, AdminAccess access) { this.identity = identity; this.access = access; }
    public record UserInput(String username, String password, boolean active, boolean adminAccess, boolean superuser, Set<String> groupIds) {}
    public record GroupInput(String name, Set<String> permissions) {}
    private Map<String, Object> view(AdminIdentityStore.Account user) {
        return Map.of("id", user.id(), "username", user.username(), "active", user.active(), "adminAccess", user.adminAccess(),
                "superuser", user.superuser(), "groupIds", user.groupIds(), "permissions", user.permissions());
    }
    @GetMapping("/permissions") public Object permissions() { identity.store(); return access.catalog(); }
    @GetMapping("/users") public Object users() { return identity.store().users().stream().map(this::view).toList(); }
    @PostMapping("/users") public Object create(@RequestBody UserInput input) { return save(null, input); }
    @PutMapping("/users/{id}") public Object update(@PathVariable String id, @RequestBody UserInput input) { return save(id, input); }
    private Object save(String id, UserInput input) {
        var store = identity.store();
        String username = name(input.username());
        String hash = input.password() == null || input.password().isEmpty() ? null : identity.encodePassword(input.password());
        if (id == null && hash == null) throw new IllegalArgumentException("Password is required");
        Set<String> groups = input.groupIds() == null ? Set.of() : input.groupIds();
        var known = store.groups().stream().map(AdminIdentityStore.Group::id).collect(java.util.stream.Collectors.toSet());
        if (!known.containsAll(groups)) throw new IllegalArgumentException("Unknown group");
        return view(store.saveUser(id, username, hash, input.active(), input.adminAccess(), input.superuser(), groups));
    }
    @DeleteMapping("/users/{id}") public void deleteUser(@PathVariable String id) { identity.store().deleteUser(id); }
    @GetMapping("/groups") public Object groups() { return identity.store().groups(); }
    @PostMapping("/groups") public Object createGroup(@RequestBody GroupInput input) { return saveGroup(null, input); }
    @PutMapping("/groups/{id}") public Object updateGroup(@PathVariable String id, @RequestBody GroupInput input) { return saveGroup(id, input); }
    private Object saveGroup(String id, GroupInput input) {
        var store = identity.store();
        Set<String> permissions = input.permissions() == null ? Set.of() : input.permissions();
        var known = access.catalog().stream().map(p -> p.get("key")).collect(java.util.stream.Collectors.toSet());
        if (!known.containsAll(permissions)) throw new IllegalArgumentException("Unknown permission key");
        return store.saveGroup(id, name(input.name()), permissions);
    }
    @DeleteMapping("/groups/{id}") public void deleteGroup(@PathVariable String id) { identity.store().deleteGroup(id); }
    private String name(String value) {
        if (value == null || value.isBlank() || value.length() > 100) throw new IllegalArgumentException("Name must contain 1–100 characters");
        return value.trim();
    }
    @ExceptionHandler(AdminAccessDeniedException.class) public ResponseEntity<?> denied() {
        return ResponseEntity.status(403).body(Map.of("code", "ACCESS_DENIED", "error", "Only superusers can manage admin security."));
    }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> invalid(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
    @ExceptionHandler(NoSuchElementException.class) public ResponseEntity<?> missing() {
        return ResponseEntity.status(404).body(Map.of("error", "User or group not found"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<?> conflict() {
        return ResponseEntity.status(409).body(Map.of("error", "Name already exists or a referenced group no longer exists"));
    }
}
