package io.github.jpaadmin.boot.config;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

public class JpaAdminWebMvcConfigurer implements WebMvcConfigurer {

    private final JpaAdminProperties properties;

    public JpaAdminWebMvcConfigurer(JpaAdminProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        String path = properties.getPath();
        registry.addRedirectViewController("/", path + "/");
        registry.addRedirectViewController(path, path + "/");
        registry.addViewController(path + "/").setViewName("forward:" + path + "/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = properties.getPath();
        String pattern = path + "/**";

        registry.addResourceHandler(pattern)
                .addResourceLocations("classpath:/META-INF/resources/admin/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        if (resourcePath == null || resourcePath.isEmpty() || "/".equals(resourcePath) || "index.html".equals(resourcePath)) {
                            return location.createRelative("index.html");
                        }
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        return location.createRelative("index.html");
                    }
                });
    }
}
