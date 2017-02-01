package io.cattle.platform.app;

import io.cattle.platform.allocator.constraint.AccountConstraintsProvider;
import io.cattle.platform.allocator.constraint.AffinityConstraintsProvider;
import io.cattle.platform.allocator.constraint.BaseConstraintsProvider;
import io.cattle.platform.allocator.constraint.NetworkContainerConstraintProvider;
import io.cattle.platform.allocator.constraint.VolumeAccessModeConstraintProvider;
import io.cattle.platform.allocator.constraint.VolumesFromConstraintProvider;
import io.cattle.platform.allocator.dao.impl.AllocatorDaoImpl;
import io.cattle.platform.allocator.eventing.impl.AllocatorServiceImpl;
import io.cattle.platform.allocator.service.AllocationHelperImpl;
import io.cattle.platform.simple.allocator.SimpleAllocator;
import io.cattle.platform.simple.allocator.dao.impl.SimpleAllocatorDaoImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AllocatorServerConfig {

    @Bean
    AllocatorServiceImpl AllocatorEventListenerImpl() {
        return new AllocatorServiceImpl();
    }

    @Bean
    AllocatorDaoImpl AllocatorDaoImpl() {
        return new AllocatorDaoImpl();
    }

    @Bean
    AllocationHelperImpl AllocatorServiceImpl() {
        return new AllocationHelperImpl();
    }

    @Bean
    BaseConstraintsProvider BaseConstraintsProvider() {
        return new BaseConstraintsProvider();
    }

    @Bean
    AccountConstraintsProvider AccountConstraintsProvider() {
        return new AccountConstraintsProvider();
    }

    @Bean
    AffinityConstraintsProvider AffinityConstraintsProvider() {
        return new AffinityConstraintsProvider();
    }

    @Bean
    VolumesFromConstraintProvider VolumesFromConstraintProvider() {
        return new VolumesFromConstraintProvider();
    }

    @Bean
    NetworkContainerConstraintProvider NetworkContainerConstraintProvider() {
        return new NetworkContainerConstraintProvider();
    }

    @Bean
    VolumeAccessModeConstraintProvider VolumeAccessModeConstraintProvider() {
        return new VolumeAccessModeConstraintProvider();
    }

    @Bean
    SimpleAllocatorDaoImpl SimpleAllocatorDaoImpl() {
        return new SimpleAllocatorDaoImpl();
    }

    @Bean
    SimpleAllocator SimpleAllocator() {
        return new SimpleAllocator();
    }

}
