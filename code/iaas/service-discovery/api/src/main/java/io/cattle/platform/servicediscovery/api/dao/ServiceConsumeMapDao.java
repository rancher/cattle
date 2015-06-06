package io.cattle.platform.servicediscovery.api.dao;

import io.cattle.platform.core.model.ServiceConsumeMap;

import java.util.List;

public interface ServiceConsumeMapDao {

    ServiceConsumeMap findMapToRemove(long serviceId, long consumedServiceId);

    ServiceConsumeMap findNonRemovedMap(long serviceId, long consumedServiceId, String linkName);

    /**
     * Lists maps to remove referencing services consumed by passed serviceId
     * 
     * @param serviceId
     * @return
     */
    List<? extends ServiceConsumeMap> findConsumedMapsToRemove(long serviceId);

    /**
     * Lists maps to remove consuming service passed by serviceId
     * 
     * @param serviceId
     * @return
     */
    List<? extends ServiceConsumeMap> findConsumingMapsToRemove(long serviceId);

    /**
     * @param serviceId
     * @return list of services consumed by the service specified
     */
    List<? extends ServiceConsumeMap> findConsumedServices(long serviceId);
    
    /**
     * @param serviceId
     * @return list of services consuming the service specified
     */
    List<? extends ServiceConsumeMap> findConsumingServices(long serviceId);
}
