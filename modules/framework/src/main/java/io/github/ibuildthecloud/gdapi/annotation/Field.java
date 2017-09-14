package io.github.ibuildthecloud.gdapi.annotation;

import io.github.ibuildthecloud.gdapi.model.FieldType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Field {

    boolean include() default true;

    FieldType type() default FieldType.NONE;

    String typeString() default "";

    String name() default "";

    String description() default "";

    int displayIndex() default 0;

    boolean create() default false;

    boolean update() default false;

    boolean password() default false;

    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;

    long minLength() default Long.MIN_VALUE;

    long maxLength() default Long.MAX_VALUE;

    String defaultValue() default "";

    boolean nullable() default false;

    boolean unique() default false;

    boolean required() default false;

    String validChars() default "";

    String invalidChars() default "";

    String transform() default "";
}
