package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;

import java.util.List;

public interface ServiceDiscoveryService extends AnnotatedEventListener {

    void removeServiceMaps(Service service);

    List<Integer> getServiceInstanceUsedSuffixes(Service service, String launchConfigName);

    boolean isActiveService(Service service);

    void cloneConsumingServices(Service fromService, Service toService);

    void setVIP(Service service);

    void releaseVip(Service service);

    void addServiceLink(Service service, ServiceLink serviceLink);

    void removeServiceLink(Service service, ServiceLink serviceLink);

    boolean isSelectorLinkMatch(String selector, Service targetService);

    boolean isSelectorContainerMatch(String selector, Instance instance);

    List<String> getServiceActiveStates();

    boolean isGlobalService(Service service);

    void setPorts(Service service);

    void releasePorts(Service service);

    void setToken(Service service);

    void removeServiceIndexes(Service service);

    String allocateIpForService(Object owner, Subnet subnet, String requestedIp);

    void allocateIpToServiceIndex(Service service, ServiceIndex serviceIndex, String requestedIp);

    void releaseIpFromServiceIndex(Service service, ServiceIndex serviceIndex);
    
    void updateHealthState(Stack stack);

    boolean isScalePolicyService(Service service);

    void setServiceIndexIp(ServiceIndex serviceIndex, String ipAddress);

    @EventHandler
    void serviceUpdate(ConfigUpdate update);

    @EventHandler
    void hostEndpointsUpdate(ConfigUpdate update);

    void reconcileServiceEndpoints(Service service);

    void reconcileHostEndpoints(Host host);

    void removeFromLoadBalancerServices(Service service);

    void registerServiceLinks(Service service);

}
