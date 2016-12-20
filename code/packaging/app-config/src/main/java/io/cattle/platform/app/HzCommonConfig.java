package io.cattle.platform.app;

import io.cattle.platform.hazelcast.dao.impl.HazelcastDaoImpl;
import io.cattle.platform.hazelcast.factory.HazelcastFactory;
import io.cattle.platform.hazelcast.membership.DBDiscoveryFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@Configuration
public class HzCommonConfig {

    @Configuration
    @Profile("hazelcast.config.basic")
    @Lazy
    class Basic {
        @Bean
        HazelcastFactory HazelcastFactory() {
            return new HazelcastFactory();
        }

        @Bean
        HazelcastInstance Hazelcast(HazelcastFactory factory) {
            return factory.newInstance();
        }

        @Bean
        HazelcastDaoImpl HazelcastDaoImpl() {
            return new HazelcastDaoImpl();
        }

        @Bean
        DBDiscoveryFactory DBDiscoveryFactory() {
            return new DBDiscoveryFactory();
        }
    }

    @Configuration
    @Profile("hazelcast.config.custom")
    @Lazy
    class Custom {
        @Bean
        HazelcastInstance Hazelcast() {
            return Hazelcast.newHazelcastInstance();
        }
    }

}
