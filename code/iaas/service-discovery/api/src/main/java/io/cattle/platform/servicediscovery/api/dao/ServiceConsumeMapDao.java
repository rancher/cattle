package io.cattle.platform.servicediscovery.api.dao;

import io.cattle.platform.core.model.ServiceConsumeMap;

import java.util.List;

public interface ServiceConsumeMapDao {

    ServiceConsumeMap findMapToRemove(long serviceId, long consumedServiceId);

    ServiceConsumeMap findNonRemovedMap(long serviceId, long consumedServiceId);

    List<? extends ServiceConsumeMap> findMapsToRemove(long serviceId);

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
