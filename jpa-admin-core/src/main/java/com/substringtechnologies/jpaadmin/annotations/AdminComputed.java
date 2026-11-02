package com.substringtechnologies.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AdminComputed {
    String label() default "";
    int order() default 100;
    boolean inList() default true;
    boolean inDetail() default true;
    /** {@link java.util.Formatter} pattern applied to the value, such as {@code "%.2f %%"} or {@code "$%,.2f"}. */
    String format() default "";
}
