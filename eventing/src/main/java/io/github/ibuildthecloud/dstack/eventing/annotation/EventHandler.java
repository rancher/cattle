package io.github.ibuildthecloud.dstack.eventing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface EventHandler {

    public static final String DEFAULT_POOL_KEY = "default";

    String name() default "";

    boolean allowQueueing() default false;

    int queueDepth() default 0;

    String poolKey() default DEFAULT_POOL_KEY;


}
