package io.cattle.platform.app;

import io.cattle.platform.archaius.eventing.impl.ArchaiusEventListenerImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArchaiusConfig {

    @Bean
    ArchaiusEventListenerImpl ArchaiusEventListenerImpl() {
        return new ArchaiusEventListenerImpl();
    }
}
