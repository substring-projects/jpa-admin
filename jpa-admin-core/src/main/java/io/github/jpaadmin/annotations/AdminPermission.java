package io.github.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AdminPermission {
    String[] roles() default {};
    String[] createRoles() default {};
    String[] readRoles() default {};
    String[] updateRoles() default {};
    String[] deleteRoles() default {};
}
