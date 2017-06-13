package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;

import java.util.List;

public interface ServiceDiscoveryService {

    /**
     * SERVICE LOGIC
     */
    void remove(Service service);

    void create(Service service);

    /**
     * LINK BASED OPERATIONS
     */
    void removeServiceLinks(Service service);

    /**
     * SERVICE INFO
     */
    boolean isSelectorContainerMatch(String selector, Instance instance);

    /**
     * ENDPOINTS UPDATE
     */

    void serviceUpdate(Long serviceId);

    void hostEndpointsUpdate(Long hostId);

    /**
     * RANDOM
     * TODO revise if can be moved to different interface(s)
     */
    void releaseIpFromServiceIndex(Service service, ServiceIndex serviceIndex);

    void updateHealthState(Long stackId);

    void setServiceIndexIp(ServiceIndex serviceIndex, String ipAddress);

    void removeFromLoadBalancerServices(Service service);
    void removeFromLoadBalancerServices(Instance instance);

    void addToBalancerService(Long serviceId, List<PortRule> rules);

}
