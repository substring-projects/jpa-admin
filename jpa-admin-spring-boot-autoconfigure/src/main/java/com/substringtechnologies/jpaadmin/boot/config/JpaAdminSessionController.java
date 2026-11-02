package com.substringtechnologies.jpaadmin.boot.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class JpaAdminSessionController {
    private final JpaAdminProperties properties;
    private final AdminIdentityService identity;
    public JpaAdminSessionController(JpaAdminProperties properties, AdminIdentityService identity) { this.properties = properties; this.identity = identity; }

    @GetMapping("${spring.jpa.admin.path:/admin}/api/auth/session")
    public Map<String, Object> session(Authentication authentication, CsrfToken csrf) {
        boolean authenticated = identity.canEnterAdmin();
        var result = new java.util.LinkedHashMap<String, Object>(Map.of("authenticated", authenticated,
                "username", authenticated ? authentication.getName() : "",
                "title", properties.getTitle(), "csrfToken", csrf.getToken(), "csrfHeader", csrf.getHeaderName(),
                "logoUrl", properties.getLogoUrl() == null ? "" : properties.getLogoUrl(),
                "primaryColor", properties.getPrimaryColor() == null ? "" : properties.getPrimaryColor(),
                "exportEnabled", properties.isExportEnabled()));
        result.put("manageSecurity", authenticated && identity.isSuperuser() && identity.databaseMode());
        return result;
    }
}
