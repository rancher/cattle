package io.cattle.platform.eventing.annotation;

import io.cattle.platform.lock.definition.LockDefinition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface EventHandler {

    public static final String DEFAULT_POOL_KEY = "default";

    String name() default "";

    Class<? extends EventNameProvider> nameProvider() default EventNameProvider.class;

    String poolKey() default DEFAULT_POOL_KEY;

    Class<? extends LockDefinition> lock() default LockDefinition.class;

}
