package com.substringtechnologies.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Turns a {@code String} field into a file upload. The field stores the uploaded file's URL.
 *
 * <pre>{@code
 * @AdminUpload(folder = "products", maxSize = "5MB")
 * private String imageUrl;
 * }</pre>
 *
 * Where files go is configured under {@code spring.jpa.admin.storage.<name>}; credentials never belong here.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AdminUpload {

    /** Name of a storage configured under {@code spring.jpa.admin.storage}. */
    String storage() default "default";

    /** Folder (key prefix) inside the storage, for example {@code "products/images"}. */
    String folder() default "";

    /**
     * Allowed MIME types. Wildcards such as {@code "image/*"} are supported, but SVG is only accepted when
     * {@code "image/svg+xml"} is listed explicitly because SVG files can contain scripts.
     */
    String[] accept() default {"image/png", "image/jpeg", "image/webp", "image/gif"};

    /** Maximum file size such as {@code "500KB"}, {@code "5MB"} or {@code "1GB"}. */
    String maxSize() default "5MB";
}
