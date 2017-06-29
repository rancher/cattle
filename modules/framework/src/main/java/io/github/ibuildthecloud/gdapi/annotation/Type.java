package io.github.ibuildthecloud.gdapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Type {

    String name() default "";

    String pluralName() default "";

    String parent() default "";

    Class<?> parentClass() default Void.class;

    boolean create() default false;

    boolean update() default false;

    boolean list() default true;

    boolean delete() default false;

    boolean byId() default true;

}
