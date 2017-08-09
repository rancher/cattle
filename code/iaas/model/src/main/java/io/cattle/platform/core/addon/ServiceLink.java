package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ServiceLink {
    // This field is not used publically through the API, only on the backend
    long consumingServiceId;
    Long serviceId;
    String name;
    String service;

    public ServiceLink() {
    }

    public ServiceLink(Long serviceId, String name, String serviceName) {
        super();
        this.serviceId = serviceId;
        this.name = name;
        this.service = serviceName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public long getConsumingServiceId() {
        return consumingServiceId;
    }

    public void setConsumingServiceId(long consumingServiceId) {
        this.consumingServiceId = consumingServiceId;
    }

    public String getUuid() {
        if (serviceId != null) {
            return serviceId + "." + (name == null ? "" : name);
        }
        return service + "." + (name == null ? "" : name);
    }

    public String getService() {
        return service;
    }

    public void setService(String serviceName) {
        this.service = serviceName;
    }
}
