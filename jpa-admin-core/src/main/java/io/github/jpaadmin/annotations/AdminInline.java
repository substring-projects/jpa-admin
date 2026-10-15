package io.github.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AdminInline {
    InlineType type() default InlineType.TABULAR;
    String title() default "";
    boolean canAdd() default true;
    boolean canDelete() default true;
    int limit() default 50;
}
