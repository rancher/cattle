package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;

import java.util.Map;

public interface ServiceDiscoveryService extends AnnotatedEventListener {

    /**
     * SERVICE LOGIC
     */
    void remove(Service service);

    void create(Service service);

    /**
     * LINK BASED OPERATIONS
     */
    void removeServiceLinks(Service service);

    void addServiceLink(Service service, ServiceLink serviceLink);

    void removeServiceLink(Service service, ServiceLink serviceLink);

    void registerServiceLinks(Service service);

    /**
     * SERVICE INFO
     */
    boolean isSelectorLinkMatch(String selector, Service targetService);

    boolean isSelectorContainerMatch(String selector, Instance instance);

    /**
     * ENDPOINTS UPDATE
     */

    @EventHandler
    void serviceUpdate(ConfigUpdate update);

    @EventHandler
    void hostEndpointsUpdate(ConfigUpdate update);

    void reconcileServiceEndpoints(Service service);

    void reconcileHostEndpoints(Host host);


    /**
     * RANDOM
     * TODO revise if can be moved to different interface(s)
     */
    void allocateIpToServiceIndex(Service service, ServiceIndex serviceIndex, String requestedIp);

    void releaseIpFromServiceIndex(Service service, ServiceIndex serviceIndex);
    
    void updateHealthState(Stack stack);

    void setServiceIndexIp(ServiceIndex serviceIndex, String ipAddress);

    void removeFromLoadBalancerServices(Service service, Instance instance);

    void incrementExecutionCount(Object object);

    void resetUpgradeFlag(Service service);

    Map<String, Object> buildServiceInstanceLaunchData(Service service, Map<String, Object> deployParams,
            String launchConfigName, AllocationHelper allocationHelper);

    void setPorts(Service service);
}
