package com.substringtechnologies.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Write-only value: never searched, filtered, sorted, exported or sent to the browser. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AdminSensitive {
    /** {@code true} shows {@link #maskPattern()} when a value is set; {@code false} doesn't reveal whether one is set. */
    boolean mask() default true;
    String maskPattern() default "••••••••";
}
