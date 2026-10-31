package com.substringtechnologies.jpaadmin.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.jpa.admin")
public class JpaAdminProperties {

    /**
     * Whether the JPA Admin dashboard is enabled. Default is true.
     */
    private boolean enabled = true;

    private final Security security = new Security();
    public Security getSecurity() { return security; }

    public static class Security {
        private String username = "admin";
        private String password = "";
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
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
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public boolean isExportEnabled() { return exportEnabled; }
    public void setExportEnabled(boolean exportEnabled) { this.exportEnabled = exportEnabled; }
}
