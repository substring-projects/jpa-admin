package com.substringtechnologies.jpaadmin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AdminLookup {
    /** Related record property shown as the label in cells and pickers. */
    String lookupProperty() default "";
    /** Text property of the related record that the picker searches. */
    String searchProperty() default "";
}
