package io.cattle.platform.configitem.context.dao.impl;

import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;

public class ServiceInstanceData {
    Service service;
    Stack stack;
    IpAddress ipAddress;
    Instance instance;
    ServiceExposeMap exposeMap;
    Nic nic;

    public ServiceInstanceData(Stack stack, Service service, IpAddress ipAddress, Instance instance,
            ServiceExposeMap exposeMap, Nic nic) {
        super();
        this.service = service;
        this.stack = stack;
        this.ipAddress = ipAddress;
        this.instance = instance;
        this.exposeMap = exposeMap;
        this.nic = nic;
    }
    public Service getService() {
        return service;
    }
    public void setService(Service service) {
        this.service = service;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }
    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public ServiceExposeMap getExposeMap() {
        return exposeMap;
    }

    public void setExposeMap(ServiceExposeMap exposeMap) {
        this.exposeMap = exposeMap;
    }

    public Nic getNic() {
        return nic;
    }

    public void setNic(Nic nic) {
        this.nic = nic;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

}
