package io.cattle.platform.app;

import io.cattle.platform.deferred.context.DeferredContextListener;

import java.util.Arrays;

import org.apache.cloudstack.managed.context.ManagedContextListener;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.managed.context.impl.DefaultManagedContext;
import org.apache.cloudstack.managed.context.impl.MdcClearListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextConfig {

    @Bean
    DefaultManagedContext DefaultManagedContext() {
        DefaultManagedContext dmc = new DefaultManagedContext();
        dmc.setListeners(Arrays.asList(
                (ManagedContextListener<?>)new DeferredContextListener(),
                new MdcClearListener()));
        ManagedContextRunnable.initializeGlobalContext(dmc);
        return dmc;
    }

}
