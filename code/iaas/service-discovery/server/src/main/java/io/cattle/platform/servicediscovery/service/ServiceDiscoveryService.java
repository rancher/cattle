package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

import java.util.List;

public interface ServiceDiscoveryService {

    void removeServiceMaps(Service service);

    List<Integer> getServiceInstanceUsedOrderIds(Service service, String launchConfigName);

    boolean isActiveService(Service service);

    void cloneConsumingServices(Service fromService, Service toService);

    void setVIP(Service service);

    void releaseVip(Service service);

    void addServiceLink(Service service, ServiceLink serviceLink);

    boolean isSelectorLinkMatch(Service sourceService, Service targetService);

    boolean isSelectorContainerMatch(Service sourceService, long instanceId);

    boolean isServiceInstance(Service service, Instance instance);

    List<String> getServiceActiveStates();

    boolean isGlobalService(Service service);

    void propagatePublicEndpoint(PublicEndpoint publicEndpoint, boolean add);

    void setPorts(Service service);

    void releasePorts(Service service);

    void setToken(Service service);
}
