package com.substringtechnologies.jpaadmin.boot.config;

import com.substringtechnologies.jpaadmin.storage.StorageSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "spring.jpa.admin")
public class JpaAdminProperties {

    /**
     * Whether the JPA Admin dashboard is enabled. Default is true.
     */
    private boolean enabled = true;

    private final Security security = new Security();
    public Security getSecurity() { return security; }

    /** Named file storages for {@code @AdminUpload} fields; a field without a storage name uses {@code default}. */
    private Map<String, StorageSettings> storage = new LinkedHashMap<>();
    public Map<String, StorageSettings> getStorage() { return storage; }
    public void setStorage(Map<String, StorageSettings> storage) { this.storage = storage; }

    private final Upload upload = new Upload();
    public Upload getUpload() { return upload; }

    public static class Upload {
        /**
         * Secret used to sign upload tickets. Set it when running several instances behind a load balancer;
         * otherwise a random secret is generated at startup.
         */
        private String signingKey = "";
        public String getSigningKey() { return signingKey; }
        public void setSigningKey(String signingKey) { this.signingKey = signingKey; }
    }

    public static class Security {
        private boolean initializeSchema;
        public boolean isInitializeSchema() { return initializeSchema; }
        public void setInitializeSchema(boolean value) { initializeSchema = value; }
        private String mode = "configured";
        public String getMode() { return mode; }
        public void setMode(String value) {
            if (!java.util.Set.of("configured", "database").contains(value)) throw new IllegalArgumentException("security.mode must be configured or database");
            mode = value;
        }
        private String username = "admin";
        private String password = "";
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        private final Login login = new Login();
        public Login getLogin() { return login; }
    }

    /** Sign-in throttling: failures are counted per username and client address, and per client address. */
    public static class Login {
        /** Failed sign-ins for one username from one address before it is locked. 0 disables throttling. */
        private int maxAttempts = 5;
        /** How long a lock lasts, and the window in which failures are counted. */
        private java.time.Duration lockDuration = java.time.Duration.ofMinutes(15);
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) {
            if (maxAttempts < 0) throw new IllegalArgumentException("security.login.max-attempts must be 0 or more");
            this.maxAttempts = maxAttempts;
        }
        public java.time.Duration getLockDuration() { return lockDuration; }
        public void setLockDuration(java.time.Duration lockDuration) {
            if (lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()) throw new IllegalArgumentException("security.login.lock-duration must be positive");
            this.lockDuration = lockDuration;
        }
    }

    /**
     * Web context path where Admin UI is accessible. Default is /admin.
     */
    private String path = "/admin";

    /**
     * Dashboard title displayed on the navigation bar and document head.
     */
    private String title = "Spring Boot JPA Admin";

    /**
     * Optional custom logo URL.
     */
    private String logoUrl = "";

    /**
     * Brand accent color as a hex value such as "#2563eb". Empty keeps the built-in theme.
     */
    private String primaryColor = "";

    /**
     * Default page size for entity tables. Default is 25.
     */
    private int pageSize = 25;

    /**
     * Whether export to CSV/Excel is enabled.
     */
    private boolean exportEnabled = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPath() { return path; }
    public void setPath(String path) {
        // Request mappings read the raw property, so it must already be in canonical form to match the security rules.
        if (path == null || !path.matches("(/[A-Za-z0-9._~-]+)+")) {
            throw new IllegalArgumentException("spring.jpa.admin.path must start with '/', have no trailing slash and not be '/' (got \"" + path + "\")");
        }
        this.path = path;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) {
        if (pageSize < 1 || pageSize > 500) throw new IllegalArgumentException("page-size must be between 1 and 500");
        this.pageSize = pageSize;
    }

    public boolean isExportEnabled() { return exportEnabled; }
    public void setExportEnabled(boolean exportEnabled) { this.exportEnabled = exportEnabled; }
}
