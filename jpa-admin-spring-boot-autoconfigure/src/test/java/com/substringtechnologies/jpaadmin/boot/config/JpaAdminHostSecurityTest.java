package com.substringtechnologies.jpaadmin.boot.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JpaAdminHostSecurityTest {
    private static final byte[] KEY = "test-only-signing-key-at-least-32-bytes-long".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/internal/console"})
    void hostAndAdminRemainIndependent(String path) throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of("spring.jpa.admin.path", path)));
            context.register(HostConfiguration.class, JpaAdminSecurityConfiguration.class);
            context.refresh();
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

            // The catch-all host chain requires authentication, but admin pages and assets still load.
            for (String suffix : new String[]{"", "/", "/assets/app.js", "/Product/1/edit"}) {
                mvc.perform(get(path + suffix)).andExpect(status().isOk());
            }
            mvc.perform(get("/host/me")).andExpect(status().isUnauthorized());
            mvc.perform(get(path + "/api/metadata")).andExpect(status().isUnauthorized());
            mvc.perform(get(path + "/api")).andExpect(status().isUnauthorized());

            // Real signed JWT validation on the host chain; JWTs do not sign users into admin.
            String token = jwt();
            mvc.perform(get("/host-token/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(content().string("jwt-user"));
            mvc.perform(get("/host-token/me").header("Authorization", "Bearer invalid"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get(path + "/api/metadata").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());

            var session = new MockHttpSession();
            CsrfToken hostCsrf = csrf(mvc, "/host/csrf", session);
            mvc.perform(post("/host/login").session(session).header(hostCsrf.getHeaderName(), hostCsrf.getToken())
                    .param("username", "host-user").param("password", "host-password"))
                    .andExpect(status().isNoContent());
            hostCsrf = csrf(mvc, "/host/csrf", session);
            mvc.perform(get("/host/me").session(session)).andExpect(content().string("host-user"));
            // Even a host user with ROLE_JPA_ADMIN must separately sign into admin.
            mvc.perform(get(path + "/api/auth/session").session(session))
                    .andExpect(jsonPath("$.authenticated").value(false));
            mvc.perform(get(path + "/api/metadata").session(session)).andExpect(status().isUnauthorized());

            CsrfToken adminCsrf = csrf(mvc, path + "/api/auth/session", session);
            mvc.perform(post(path + "/api/auth/login").session(session)
                    .header(hostCsrf.getHeaderName(), hostCsrf.getToken())
                    .param("username", "admin-user").param("password", "admin-password"))
                    .andExpect(status().isForbidden());
            mvc.perform(post(path + "/api/auth/login").session(session)
                    .header(adminCsrf.getHeaderName(), adminCsrf.getToken())
                    .param("username", "admin-user").param("password", "admin-password"))
                    .andExpect(status().isNoContent());
            mvc.perform(get(path + "/api/auth/session").session(session))
                    .andExpect(jsonPath("$.username").value("admin-user"));
            mvc.perform(get("/host/me").session(session)).andExpect(content().string("host-user"));
            mvc.perform(post("/host/write").session(session).header(hostCsrf.getHeaderName(), hostCsrf.getToken()))
                    .andExpect(status().isOk());
            mvc.perform(get("/host-token/me").session(session)).andExpect(status().isUnauthorized());

            adminCsrf = csrf(mvc, path + "/api/auth/session", session);
            mvc.perform(post(path + "/api/write").session(session)).andExpect(status().isForbidden());
            mvc.perform(post(path + "/api/write").session(session).header(adminCsrf.getHeaderName(), adminCsrf.getToken()))
                    .andExpect(status().isOk());
            mvc.perform(post(path + "/api/auth/logout").session(session).header(adminCsrf.getHeaderName(), adminCsrf.getToken()))
                    .andExpect(status().isNoContent());
            assertFalse(session.isInvalid());
            mvc.perform(get(path + "/api/metadata").session(session)).andExpect(status().isUnauthorized());
            mvc.perform(get("/host/me").session(session)).andExpect(content().string("host-user"));
            mvc.perform(post("/host/write").session(session).header(hostCsrf.getHeaderName(), hostCsrf.getToken()))
                    .andExpect(status().isOk());

            // Admin-only login must not grant host session access either.
            var adminOnly = new MockHttpSession();
            var csrf = csrf(mvc, path + "/api/auth/session", adminOnly);
            mvc.perform(post(path + "/api/auth/login").session(adminOnly).header(csrf.getHeaderName(), csrf.getToken())
                    .param("username", "admin-user").param("password", "admin-password"))
                    .andExpect(status().isNoContent());
            mvc.perform(get("/host/me").session(adminOnly)).andExpect(status().isUnauthorized());
        }
    }

    private static CsrfToken csrf(MockMvc mvc, String url, MockHttpSession session) throws Exception {
        var result = mvc.perform(get(url).session(session)).andExpect(status().isOk()).andReturn();
        var token = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        assertNotNull(token);
        token.getToken();
        return token;
    }

    private static String jwt() throws Exception {
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), new JWTClaimsSet.Builder()
                .subject("jwt-user").issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(120))).build());
        jwt.sign(new MACSigner(KEY));
        return jwt.serialize();
    }

    @Configuration @EnableWebMvc
    static class HostConfiguration {
        @Bean JpaAdminProperties properties(Environment environment) {
            var properties = new JpaAdminProperties();
            properties.setPath(environment.getRequiredProperty("spring.jpa.admin.path"));
            properties.getSecurity().setUsername("admin-user");
            properties.getSecurity().setPassword("admin-password");
            return properties;
        }
        @Bean @Order(0) SecurityFilterChain jwtChain(HttpSecurity http) throws Exception {
            return http.securityMatcher("/host-token/**")
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .csrf(c -> c.disable())
                    .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(NimbusJwtDecoder.withSecretKey(new SecretKeySpec(KEY, "HmacSHA256")).build())))
                    .authorizeHttpRequests(a -> a.anyRequest().authenticated()).build();
        }
        @Bean @Order(1) SecurityFilterChain hostChain(HttpSecurity http) throws Exception {
            var encoder = new BCryptPasswordEncoder();
            var provider = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(User.withUsername("host-user")
                    .password(encoder.encode("host-password")).roles("JPA_ADMIN", "HOST").build()));
            provider.setPasswordEncoder(encoder);
            return http.authenticationManager(new ProviderManager(provider))
                    .authorizeHttpRequests(a -> a.requestMatchers("/host/csrf", "/host/login").permitAll().anyRequest().authenticated())
                    .formLogin(f -> f.loginProcessingUrl("/host/login").successHandler((req, res, auth) -> res.setStatus(204)))
                    .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.setStatus(401))).build();
        }
        @Bean Endpoints endpoints() { return new Endpoints(); }
    }

    @RestController
    static class Endpoints {
        @GetMapping({"${spring.jpa.admin.path}", "${spring.jpa.admin.path}/", "${spring.jpa.admin.path}/assets/app.js", "${spring.jpa.admin.path}/Product/1/edit"})
        String page() { return "ui"; }
        @GetMapping("${spring.jpa.admin.path}/api/metadata") Map<String, String> metadata() { return Map.of("ok", "true"); }
        @PostMapping("${spring.jpa.admin.path}/api/write") String adminWrite() { return "ok"; }
        @GetMapping({"/host/me", "/host-token/me"}) String me(Authentication authentication) { return authentication.getName(); }
        @GetMapping("/host/csrf") String csrf(CsrfToken token) { return token.getToken(); }
        @PostMapping("/host/write") String write() { return "ok"; }
    }
}
