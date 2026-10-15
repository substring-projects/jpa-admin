package io.github.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AdminAction {
    String name();
    String icon() default "zap";
    String confirmation() default "";
    ActionScope scope() default ActionScope.BOTH;
}
