package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;

public class ServiceDnsEntryData {
    Service clientService;
    Service targetService;
    ServiceConsumeMap consumeMap;

    public Service getClientService() {
        return clientService;
    }

    public void setClientService(Service clientService) {
        this.clientService = clientService;
    }

    public Service getTargetService() {
        return targetService;
    }

    public void setTargetService(Service targetService) {
        this.targetService = targetService;
    }

    public ServiceConsumeMap getConsumeMap() {
        return consumeMap;
    }

    public void setConsumeMap(ServiceConsumeMap consumeMap) {
        this.consumeMap = consumeMap;
    }

    public ServiceDnsEntryData(Service clientService, Service targetService, ServiceConsumeMap consumeMap) {
        super();
        this.clientService = clientService;
        this.targetService = targetService;
        this.consumeMap = consumeMap;
    }

}
