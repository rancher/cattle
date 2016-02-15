package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;

public class ServiceDnsEntryData {
    Environment clientStack;
    Service clientService;
    Service targetService;
    Environment targetStack;
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

    public ServiceDnsEntryData(Service clientService, Service targetService, ServiceConsumeMap consumeMap,
            Environment clientStack, Environment targetStack) {
        super();
        this.clientService = clientService;
        this.targetService = targetService;
        this.consumeMap = consumeMap;
        this.clientStack = clientStack;
        this.targetStack = targetStack;
    }

    public Environment getClientStack() {
        return clientStack;
    }

    public void setClientStack(Environment clientStack) {
        this.clientStack = clientStack;
    }

    public Environment getTargetStack() {
        return targetStack;
    }

    public void setTargetStack(Environment targetStack) {
        this.targetStack = targetStack;
    }

}
