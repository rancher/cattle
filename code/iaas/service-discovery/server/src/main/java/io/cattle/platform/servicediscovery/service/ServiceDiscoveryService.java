package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;

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
     * RANDOM
     * TODO revise if can be moved to different interface(s)
     */
    void releaseIpFromServiceIndex(Service service, ServiceIndex serviceIndex);

    void updateHealthState(Long stackId);

    void setServiceIndexIp(ServiceIndex serviceIndex, String ipAddress);

}
