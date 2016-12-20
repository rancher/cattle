package io.cattle.platform.app;

import io.cattle.platform.extension.spring.ExtensionDiscovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({"classpath:META-INF/cattle/process/spring-process-context.xml"})
public class ProcessConfig {

    @Bean
    ExtensionDiscovery ExtensionDiscovery() {
        return new ExtensionDiscovery();
    }

}
