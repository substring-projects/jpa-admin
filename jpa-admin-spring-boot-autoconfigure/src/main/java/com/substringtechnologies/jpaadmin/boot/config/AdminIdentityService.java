package com.substringtechnologies.jpaadmin.boot.config;

import com.substringtechnologies.jpaadmin.security.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;

/** Resolves current account state on every call so group revocation does not wait for the next login. */
public class AdminIdentityService implements AdminAuthorization {
    private final AdminIdentityStore store;
    private final JpaAdminProperties.Security config;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String configuredHash;
    public AdminIdentityService(JpaAdminProperties properties, AdminIdentityStore store) {
        config = properties.getSecurity();
        this.store = store;
        if (config.getMode().equals("database") && store == null)
            throw new IllegalStateException("Database admin security requires jpa-admin-identity or an AdminIdentityStore bean");
        if (config.getMode().equals("configured")) {
            if (config.getUsername() == null || config.getUsername().isBlank() || config.getPassword() == null || config.getPassword().isBlank())
                throw new IllegalStateException("Configure spring.jpa.admin.security.username and spring.jpa.admin.security.password");
            configuredHash = encoder.encode(config.getPassword());
        } else {
            configuredHash = null;
            store.bootstrap(config.getUsername(), config.getPassword() == null || config.getPassword().isBlank() ? null : encoder.encode(config.getPassword()));
        }
    }
    public boolean databaseMode() { return config.getMode().equals("database"); }
    public AdminIdentityStore store() {
        requireSuperuser();
        if (!databaseMode()) throw new IllegalArgumentException("User management requires database security mode");
        return store;
    }
    public UserDetails loadUser(String username) {
        if (!databaseMode()) {
            if (!config.getUsername().equals(username)) throw new UsernameNotFoundException("Invalid credentials");
            return User.withUsername(username).password(configuredHash).roles("JPA_ADMIN").build();
        }
        var user = store.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return new DatabaseUser(user);
    }
    private record Resolved(String id, long version, Optional<AdminIdentityStore.Account> account) {}
    private Optional<AdminIdentityStore.Account> current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof DatabaseUser principal)) return Optional.empty();
        // A request sees one authorization snapshot; the next request always resolves fresh grants.
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        String cacheKey = AdminIdentityService.class.getName() + ".account";
        if (attributes != null && attributes.getAttribute(cacheKey, 0) instanceof Resolved cached
                && cached.id().equals(principal.id) && cached.version() == principal.version) return cached.account();
        var account = store.findById(principal.id).filter(a -> a.active() && a.adminAccess() && a.version() == principal.version);
        if (attributes != null) attributes.setAttribute(cacheKey, new Resolved(principal.id, principal.version, account), 0);
        return account;
    }
    public boolean canEnterAdmin() {
        if (databaseMode()) return current().isPresent();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && config.getUsername().equals(auth.getName())
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_JPA_ADMIN"));
    }
    public boolean isSuperuser() { return databaseMode() ? current().map(AdminIdentityStore.Account::superuser).orElse(false) : canEnterAdmin(); }
    public String subject() {
        if (!canEnterAdmin()) throw new AdminAccessDeniedException();
        return databaseMode() ? current().orElseThrow(AdminAccessDeniedException::new).id() : "configured:" + config.getUsername();
    }
    public boolean isAllowed(String resource, String operation) {
        if (!databaseMode()) return canEnterAdmin();
        return current().map(a -> a.superuser() || a.permissions().contains(resource + ":" + operation)).orElse(false);
    }
    public String encodePassword(String password) {
        if (password == null || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Password must contain at least 12 characters and at most 72 UTF-8 bytes");
        return encoder.encode(password);
    }
    public static final class DatabaseUser extends User {
        private final String id;
        private final long version;
        DatabaseUser(AdminIdentityStore.Account user) {
            super(user.username(), user.passwordHash(), user.active() && user.adminAccess(), true, true, true,
                    List.of(new SimpleGrantedAuthority("ROLE_JPA_ADMIN")));
            id = user.id(); version = user.version();
        }
    }
}
