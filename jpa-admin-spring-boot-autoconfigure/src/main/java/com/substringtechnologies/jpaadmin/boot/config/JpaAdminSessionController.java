package com.substringtechnologies.jpaadmin.boot.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class JpaAdminSessionController {
    private final JpaAdminProperties properties;
    public JpaAdminSessionController(JpaAdminProperties properties) { this.properties = properties; }

    @GetMapping("${spring.jpa.admin.path:/admin}/api/auth/session")
    public Map<String, Object> session(Authentication authentication, CsrfToken csrf) {
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_JPA_ADMIN"));
        return Map.of("authenticated", authenticated,
                "username", authenticated ? authentication.getName() : "",
                "title", properties.getTitle(), "csrfToken", csrf.getToken(), "csrfHeader", csrf.getHeaderName(),
                "logoUrl", properties.getLogoUrl() == null ? "" : properties.getLogoUrl(),
                "primaryColor", properties.getPrimaryColor() == null ? "" : properties.getPrimaryColor(),
                "exportEnabled", properties.isExportEnabled());
    }
}
