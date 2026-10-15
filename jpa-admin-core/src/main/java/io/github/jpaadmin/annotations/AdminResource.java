package io.github.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AdminResource {
    String displayName() default "";
    String pluralName() default "";
    String group() default "General";
    String icon() default "database";
    int order() default 100;
    boolean readOnly() default false;
    boolean creatable() default true;
    boolean updatable() default true;
    boolean deletable() default true;
    boolean exportable() default true;
    String defaultSortBy() default "id";
    SortDir defaultSortDir() default SortDir.DESC;
    int pageSize() default 25;
    boolean hidden() default false;
}
