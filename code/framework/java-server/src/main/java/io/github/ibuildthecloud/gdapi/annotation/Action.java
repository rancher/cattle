package io.github.ibuildthecloud.gdapi.annotation;

public @interface Action {
    String name();

    Class<?> input() default Void.class;

    Class<?> output() default Void.class;

    String inputType() default "";

    String outputType() default "";

    boolean collection() default false;
}
