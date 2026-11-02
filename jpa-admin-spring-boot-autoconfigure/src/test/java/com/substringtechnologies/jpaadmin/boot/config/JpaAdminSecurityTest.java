package com.substringtechnologies.jpaadmin.boot.config;

import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class JpaAdminSecurityTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class, JpaAdminSecurityConfiguration.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void close() { context.close(); }

    @Test void anonymousCannotReadMetadataOrRecords() throws Exception {
        mvc.perform(get("/admin/api/metadata")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/api/Product")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/api/auth/session")).andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.csrfToken").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void loginUsesConfiguredCredentialsAndLogoutClearsAdminContext() throws Exception {
        mvc.perform(post("/admin/api/auth/login").with(csrf()).param("username", "admin").param("password", "admin"))
                .andExpect(status().isUnauthorized());
        var result = mvc.perform(post("/admin/api/auth/login").with(csrf()).param("username", "configured-admin").param("password", "test-password"))
                .andExpect(status().isNoContent()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/admin/api/metadata").session(session)).andExpect(status().isOk());
        mvc.perform(get("/admin/api/auth/session").session(session)).andExpect(jsonPath("$.username").value("configured-admin"));
        mvc.perform(post("/admin/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertFalse(session.isInvalid());
        mvc.perform(get("/admin/api/metadata").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/api/metadata")).andExpect(status().isUnauthorized());
    }
    @Test void csrfRequiredForLoginAndAuthenticatedWrites() throws Exception {
        mvc.perform(post("/admin/api/auth/login").param("username", "configured-admin").param("password", "test-password"))
                .andExpect(status().isForbidden());
        var result = mvc.perform(post("/admin/api/auth/login").with(csrf()).param("username", "configured-admin").param("password", "test-password"))
                .andExpect(status().isNoContent()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(post("/admin/api/Product").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/api/Product").session(session).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/admin/api/auth/logout").session(session)).andExpect(status().isForbidden());
    }
    private org.springframework.test.web.servlet.request.RequestPostProcessor from(String address) {
        return request -> { request.setRemoteAddr(address); return request; };
    }
    @Test void repeatedFailuresLockSignInForThatUsernameAndAddress() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/admin/api/auth/login").with(csrf()).with(from("10.0.0.1")).param("username", "configured-admin").param("password", "wrong"))
                    .andExpect(status().isUnauthorized());
        }
        // Even the right password is refused while locked, and the response says when to retry.
        mvc.perform(post("/admin/api/auth/login").with(csrf()).with(from("10.0.0.1")).param("username", "configured-admin").param("password", "test-password"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("LOGIN_LOCKED"));
        // Someone else's failures can't lock the real admin out from another address.
        mvc.perform(post("/admin/api/auth/login").with(csrf()).with(from("10.0.0.2")).param("username", "configured-admin").param("password", "test-password"))
                .andExpect(status().isNoContent());
    }
    @Configuration @EnableWebMvc
    static class TestConfiguration {
        @Bean JpaAdminProperties properties() {
            var properties = new JpaAdminProperties();
            properties.getSecurity().setUsername("configured-admin");
            properties.getSecurity().setPassword("test-password");
            return properties;
        }
        @Bean TestController controller() { return new TestController(); }
    }
    @RestController static class TestController {
        @GetMapping("/admin/api/metadata") Map<String, Object> metadata() { return Map.of("ok", true); }
        @PostMapping("/admin/api/Product") Map<String, Object> create() { return Map.of("ok", true); }
    }
}
