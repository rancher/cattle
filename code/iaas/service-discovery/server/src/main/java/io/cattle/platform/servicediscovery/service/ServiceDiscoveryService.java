package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Subnet;

import java.util.List;

public interface ServiceDiscoveryService {

    void removeServiceMaps(Service service);

    List<Integer> getServiceInstanceUsedSuffixes(Service service, String launchConfigName);

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

    void removeServiceIndexes(Service service);

    String allocateIpForService(Object owner, Subnet subnet, String requestedIp);

    void allocateIpToServiceIndex(ServiceIndex serviceIndex, String requestedIp);

    void releaseIpFromServiceIndex(ServiceIndex serviceIndex);
    
    void updateHealthState(List<? extends Service> services);

}
