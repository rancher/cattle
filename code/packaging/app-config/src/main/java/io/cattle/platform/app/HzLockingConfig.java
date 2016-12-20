package io.cattle.platform.app;

import io.cattle.platform.hazelcast.lock.HazelcastLockProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("hazelcast.lock")
public class HzLockingConfig {

    @Bean
    HazelcastLockProvider LockProvider() {
        return new HazelcastLockProvider();
    }

}
