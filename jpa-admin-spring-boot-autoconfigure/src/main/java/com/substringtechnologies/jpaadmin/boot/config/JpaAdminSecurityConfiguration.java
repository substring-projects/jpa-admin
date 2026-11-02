package com.substringtechnologies.jpaadmin.boot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class JpaAdminSecurityConfiguration {
    @Bean
    @Order(-100)
    public SecurityFilterChain jpaAdminSecurityFilterChain(HttpSecurity http, JpaAdminProperties properties) throws Exception {
        var config = properties.getSecurity();
        if (config.getUsername() == null || config.getUsername().isBlank()
                || config.getPassword() == null || config.getPassword().isBlank()) {
            throw new IllegalStateException("Configure spring.jpa.admin.security.username and spring.jpa.admin.security.password");
        }
        var encoder = new BCryptPasswordEncoder();
        var users = new InMemoryUserDetailsManager(User.withUsername(config.getUsername())
                .password(encoder.encode(config.getPassword())).roles("JPA_ADMIN").build());
        String path = properties.getPath();
        String api = path + "/api";
        // Keep host authentication and CSRF state intact, even in a shared servlet session.
        var contexts = new HttpSessionSecurityContextRepository();
        contexts.setSpringSecurityContextKey("JPA_ADMIN_SECURITY_CONTEXT:" + path);
        var csrf = new HttpSessionCsrfTokenRepository();
        csrf.setSessionAttributeName("JPA_ADMIN_CSRF_TOKEN:" + path);
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        // A local authentication manager keeps host application's users and providers separate.
        http.securityMatcher(path, path + "/**")
                .securityContext(context -> context.securityContextRepository(contexts))
                .csrf(configurer -> configurer.csrfTokenRepository(csrf))
                .authenticationManager(new ProviderManager(provider))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(api + "/auth/session", api + "/auth/login").permitAll()
                        .requestMatchers(api, api + "/**").hasRole("JPA_ADMIN")
                        .requestMatchers(HttpMethod.GET, path, path + "/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, path, path + "/**").permitAll()
                        .anyRequest().denyAll())
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.loginProcessingUrl(api + "/auth/login")
                        .successHandler((request, response, authentication) -> response.setStatus(204))
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Invalid username or password\"}");
                        }))
                .logout(logout -> logout.logoutUrl(api + "/auth/logout")
                        .invalidateHttpSession(false).clearAuthentication(true)
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Sign in to continue\"}");
                }));
        // Default session-based CSRF protection also covers login and logout.
        return http.build();
    }

    @Bean
    public JpaAdminSessionController jpaAdminSessionController(JpaAdminProperties properties) {
        return new JpaAdminSessionController(properties);
    }
}
