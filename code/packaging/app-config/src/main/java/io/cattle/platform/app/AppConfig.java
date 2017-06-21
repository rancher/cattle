package io.cattle.platform.app;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

@Configuration
@Import({ConfigConfig.class, ContextConfig.class,
    SystemConfig.class, CoreModelConfig.class, TypesConfig.class,
    SystemServicesConfig.class, AgentServerConfig.class,
    ArchaiusConfig.class, AllocatorServerConfig.class, ApiServerConfig.class, IaasApiConfig.class,
    ProcessConfig.class, HzCommonConfig.class, HzEventingConfig.class,
    HzLockingConfig.class})
@ComponentScans({
    @ComponentScan("io.cattle.platform.inator"),
    @ComponentScan("io.cattle.platform.loop"),
})
@ImportResource({"classpath:META-INF/cattle/legacy/spring-legacy-context.xml"})
public class AppConfig {
}
