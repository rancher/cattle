package io.cattle.platform.loop;

import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

public class HostEndpointLoop implements Loop {

    ServiceDiscoveryService sdService;
    Long hostId;

    public HostEndpointLoop(ServiceDiscoveryService sdService, Long id) {
        super();
        this.sdService = sdService;
        this.hostId = id;
    }

    @Override
    public Result run(Object input) {
        sdService.hostEndpointsUpdate(hostId);
        return Result.DONE;
    }

}
