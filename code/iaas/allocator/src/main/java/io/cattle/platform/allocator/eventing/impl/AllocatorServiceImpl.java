package io.cattle.platform.allocator.eventing.impl;

import io.cattle.platform.allocator.eventing.AllocatorService;
import io.cattle.platform.allocator.service.Allocator;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocatorServiceImpl implements AllocatorService {

    private static final Logger log = LoggerFactory.getLogger(AllocatorServiceImpl.class);

    @Inject
    Allocator allocator;

    @Override
    public void instanceAllocate(Instance instance) {
        log.info("Allocating instance [{}]", instance.getId());
        allocator.allocate(instance);
        log.info("Allocator [{}] handled request for instance [{}]", allocator, instance.getId());
    }

    @Override
    public void instanceDeallocate(Instance instance) {
        log.info("Deallocating instance [{}]", instance.getId());
        allocator.deallocate(instance);
        log.info("Deallocator [{}] handled request for instance [{}]", allocator, instance.getId());
    }

    @Override
    public void volumeDeallocate(Volume volume) {
        log.info("Deallocating volume [{}]", volume.getId());
        allocator.deallocate(volume);
        log.info("Deallocator [{}] handled request for volume [{}]", allocator, volume.getId());
    }
}
