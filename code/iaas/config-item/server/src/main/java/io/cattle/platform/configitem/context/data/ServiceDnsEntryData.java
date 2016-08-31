package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;

public class ServiceDnsEntryData {
    Stack clientStack;
    Service clientService;
    Service targetService;
    Stack targetStack;
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
            Stack clientStack, Stack targetStack) {
        super();
        this.clientService = clientService;
        this.targetService = targetService;
        this.consumeMap = consumeMap;
        this.clientStack = clientStack;
        this.targetStack = targetStack;
    }

    public Stack getClientStack() {
        return clientStack;
    }

    public void setClientStack(Stack clientStack) {
        this.clientStack = clientStack;
    }

    public Stack getTargetStack() {
        return targetStack;
    }

    public void setTargetStack(Stack targetStack) {
        this.targetStack = targetStack;
    }

}
