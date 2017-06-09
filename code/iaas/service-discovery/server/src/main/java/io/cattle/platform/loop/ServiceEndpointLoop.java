package io.cattle.platform.loop;

import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

public class ServiceEndpointLoop implements Loop {

    ServiceDiscoveryService sdService;
    Long serviceId;

    public ServiceEndpointLoop(ServiceDiscoveryService sdService, Long id) {
        super();
        this.sdService = sdService;
        this.serviceId = id;
    }

    @Override
    public Result run(Object input) {
        sdService.serviceUpdate(serviceId);
        return Result.DONE;
    }

}
