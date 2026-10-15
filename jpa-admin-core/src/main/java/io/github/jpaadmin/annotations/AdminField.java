package io.github.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AdminField {
    String label() default "";
    String description() default "";
    int order() default 100;
    boolean inList() default true;
    boolean inForm() default true;
    boolean inDetail() default true;
    boolean readOnly() default false;
    boolean searchable() default false;
    boolean sortable() default true;
    boolean filterable() default false;
    String placeholder() default "";
}
