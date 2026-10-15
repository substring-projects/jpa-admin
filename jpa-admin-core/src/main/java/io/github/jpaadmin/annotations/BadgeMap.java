package io.github.jpaadmin.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface BadgeMap {
    String value();
    BadgeColor color() default BadgeColor.DEFAULT;
    String label() default "";
}
