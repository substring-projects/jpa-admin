package com.substringtechnologies.jpaadmin.boot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class JpaAdminSecurityConfiguration {
    @Bean
    @Order(-100)
    public SecurityFilterChain jpaAdminSecurityFilterChain(HttpSecurity http, JpaAdminProperties properties, AdminIdentityService identity,
            LoginAttemptLimiter loginAttempts) throws Exception {
        String path = properties.getPath();
        String api = path + "/api";
        // Keep host authentication and CSRF state intact, even in a shared servlet session.
        var contexts = new HttpSessionSecurityContextRepository();
        contexts.setSpringSecurityContextKey("JPA_ADMIN_SECURITY_CONTEXT:" + path);
        var csrf = new HttpSessionCsrfTokenRepository();
        csrf.setSessionAttributeName("JPA_ADMIN_CSRF_TOKEN:" + path);
        var provider = new DaoAuthenticationProvider(identity::loadUser);
        provider.setPasswordEncoder(new BCryptPasswordEncoder());
        // A local authentication manager keeps host application's users and providers separate.
        http.securityMatcher(path, path + "/**")
                .securityContext(context -> context.securityContextRepository(contexts))
                .csrf(configurer -> configurer.csrfTokenRepository(csrf))
                .authenticationManager(new ProviderManager(loginAttempts.guard(provider)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(api + "/auth/session", api + "/auth/login").permitAll()
                        .requestMatchers(api, api + "/**").access((authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(identity.canEnterAdmin()))
                        .requestMatchers(HttpMethod.GET, path, path + "/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, path, path + "/**").permitAll()
                        .anyRequest().denyAll())
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.loginProcessingUrl(api + "/auth/login")
                        .successHandler((request, response, authentication) -> response.setStatus(204))
                        .failureHandler((request, response, exception) -> {
                            response.setContentType("application/json");
                            if (exception instanceof LoginAttemptLimiter.LockedException locked) {
                                long minutes = Math.max(1, (locked.getRetryAfter().toSeconds() + 59) / 60);
                                response.setStatus(429);
                                response.setHeader("Retry-After", String.valueOf(Math.max(1, locked.getRetryAfter().toSeconds())));
                                response.getWriter().write("{\"code\":\"LOGIN_LOCKED\",\"error\":\"Too many failed sign-in attempts. Try again in "
                                        + minutes + (minutes == 1 ? " minute" : " minutes") + ".\"}");
                                return;
                            }
                            response.setStatus(401);
                            response.getWriter().write("{\"error\":\"Invalid username or password\"}");
                        }))
                .logout(logout -> logout.logoutUrl(api + "/auth/logout")
                        .invalidateHttpSession(false).clearAuthentication(true)
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    boolean csrfFailure = exception instanceof org.springframework.security.web.csrf.CsrfException;
                    response.getWriter().write(csrfFailure ? "{\"code\":\"CSRF_INVALID\",\"error\":\"Request could not be verified. Please try again.\"}"
                            : "{\"code\":\"ACCESS_DENIED\",\"error\":\"Admin access is not allowed.\"}");
                }).authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"code\":\"SESSION_EXPIRED\",\"error\":\"Sign in to continue\"}");
                }));
        // Default session-based CSRF protection also covers login and logout.
        return http.build();
    }

    @Bean
    public LoginAttemptLimiter jpaAdminLoginAttemptLimiter(JpaAdminProperties properties) {
        var login = properties.getSecurity().getLogin();
        return new LoginAttemptLimiter(login.getMaxAttempts(), login.getLockDuration(), java.time.Clock.systemUTC());
    }

    @Bean
    public AdminIdentityService adminIdentityService(JpaAdminProperties properties,
            org.springframework.beans.factory.ObjectProvider<com.substringtechnologies.jpaadmin.security.AdminIdentityStore> stores) {
        return new AdminIdentityService(properties, stores.getIfAvailable());
    }

    @Bean
    public JpaAdminSessionController jpaAdminSessionController(JpaAdminProperties properties, AdminIdentityService identity) {
        return new JpaAdminSessionController(properties, identity);
    }
}
