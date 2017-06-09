package io.cattle.platform.loop;

import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

public class StackHealthLoop implements Loop {

    ServiceDiscoveryService sdService;
    Long stackId;

    public StackHealthLoop(ServiceDiscoveryService sdService, Long id) {
        super();
        this.sdService = sdService;
        this.stackId = id;
    }

    @Override
    public Result run(Object input) {
        sdService.updateHealthState(stackId);
        return Result.DONE;
    }

}
