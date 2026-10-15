package io.github.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AdminWidget {
    WidgetType type() default WidgetType.AUTO;
    String previewSize() default "48px";
    double min() default Double.MIN_VALUE;
    double max() default Double.MAX_VALUE;
    double step() default 1.0;
    String language() default "json";
}
