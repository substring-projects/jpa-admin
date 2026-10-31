package com.substringtechnologies.jpaadmin.boot.config;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JpaAdminWebMvcConfigurer implements WebMvcConfigurer {

    private static final String BUILT_BASE = "<base href=\"/admin/\"";

    private final JpaAdminProperties properties;
    private volatile Resource indexPage;

    public JpaAdminWebMvcConfigurer(JpaAdminProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        String path = properties.getPath();
        registry.addRedirectViewController(path, path + "/");
        registry.addViewController(path + "/").setViewName("forward:" + path + "/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(properties.getPath() + "/**")
                .addResourceLocations("classpath:/META-INF/resources/admin/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        if (resourcePath != null && !resourcePath.isEmpty() && !"index.html".equals(resourcePath)) {
                            Resource requested = location.createRelative(resourcePath);
                            if (requested.exists() && requested.isReadable()) {
                                return requested;
                            }
                        }
                        // Unknown paths are client-side routes, so they all get the single-page app shell.
                        return indexPage(location);
                    }
                });
    }

    /** The UI is built for /admin/; rewriting its base href lets the same bundle run under any configured path. */
    private Resource indexPage(Resource location) throws IOException {
        Resource cached = indexPage;
        if (cached != null) {
            return cached;
        }
        Resource source = location.createRelative("index.html");
        if (!source.exists()) {
            return null;
        }
        String html = new String(source.getContentAsByteArray(), StandardCharsets.UTF_8)
                .replace(BUILT_BASE, "<base href=\"" + properties.getPath() + "/\"");
        long lastModified = System.currentTimeMillis();
        cached = new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8), "JPA Admin index page") {
            @Override
            public String getFilename() {
                return "index.html";
            }

            @Override
            public long lastModified() {
                return lastModified;
            }
        };
        indexPage = cached;
        return cached;
    }
}
