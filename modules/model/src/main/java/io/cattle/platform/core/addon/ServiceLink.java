package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ServiceLink {
    // This field is not used publically through the API, only on the backend
    long consumingServiceId;
    long serviceId;
    String name;

    public ServiceLink() {
    }

    public ServiceLink(long serviceId, String name) {
        super();
        this.serviceId = serviceId;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getServiceId() {
        return serviceId;
    }

    public void setServiceId(long serviceId) {
        this.serviceId = serviceId;
    }

    public long getConsumingServiceId() {
        return consumingServiceId;
    }

    public void setConsumingServiceId(long consumingServiceId) {
        this.consumingServiceId = consumingServiceId;
    }

    public String getUuid() {
        return serviceId + "." + (name == null ? "" : name);
    }
}
