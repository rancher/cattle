package io.cattle.platform.app;

import io.cattle.platform.allocator.constraint.provider.AccountConstraintsProvider;
import io.cattle.platform.allocator.constraint.provider.AffinityConstraintsProvider;
import io.cattle.platform.allocator.constraint.provider.BaseConstraintsProvider;
import io.cattle.platform.allocator.constraint.provider.PortsConstraintProvider;
import io.cattle.platform.allocator.constraint.provider.VolumeAccessModeConstraintProvider;
import io.cattle.platform.allocator.dao.impl.AllocatorDaoImpl;
import io.cattle.platform.allocator.service.AllocationHelperImpl;
import io.cattle.platform.allocator.service.impl.AllocatorServiceImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AllocatorServerConfig {

    @Bean
    AllocatorServiceImpl AllocatorServiceImpl() {
        return new AllocatorServiceImpl();

    }

    @Bean
    AllocatorDaoImpl AllocatorDaoImpl() {
        return new AllocatorDaoImpl();
    }

    @Bean
    AllocationHelperImpl AllocationHelperImpl() {
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
    PortsConstraintProvider PortsConstraintProvider() {
        return new PortsConstraintProvider();
    }

    @Bean
    AffinityConstraintsProvider AffinityConstraintsProvider() {
        return new AffinityConstraintsProvider();
    }

    @Bean
    VolumeAccessModeConstraintProvider VolumeAccessModeConstraintProvider() {
        return new VolumeAccessModeConstraintProvider();
    }
}
