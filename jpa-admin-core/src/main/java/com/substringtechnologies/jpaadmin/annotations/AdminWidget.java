package com.substringtechnologies.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AdminWidget {
    WidgetType type() default WidgetType.AUTO;
    /** CSS size of image thumbnails in lists, such as {@code "64px"} or {@code "4rem"}. Empty keeps the default size. */
    String previewSize() default "";
    /** Lowest accepted number; enforced on save. Required for {@link WidgetType#SLIDER} together with {@link #max()}. */
    double min() default Double.NEGATIVE_INFINITY;
    /** Highest accepted number; enforced on save. */
    double max() default Double.POSITIVE_INFINITY;
    /** Input increment for numbers. {@code 0} allows any value. */
    double step() default 0;
    /** {@link WidgetType#CODE_EDITOR} syntax: json, javascript, typescript, html, css, sql, xml, markdown, yaml, python or text. */
    String language() default "json";
}
