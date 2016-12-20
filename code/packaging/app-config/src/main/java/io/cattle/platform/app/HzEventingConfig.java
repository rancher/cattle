package io.cattle.platform.app;

import io.cattle.platform.hazelcast.eventing.HazelcastEventService;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("hazelcast.eventing")
public class HzEventingConfig {

    @Bean
    HazelcastEventService EventService(@Qualifier("CoreExecutorService") ExecutorService es) {
        HazelcastEventService service = new HazelcastEventService();
        service.setExecutorService(es);
        return service;
    }

}
