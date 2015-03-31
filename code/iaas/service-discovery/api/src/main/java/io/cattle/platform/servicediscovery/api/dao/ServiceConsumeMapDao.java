package io.cattle.platform.servicediscovery.api.dao;

import io.cattle.platform.core.model.ServiceConsumeMap;

import java.util.List;

public interface ServiceConsumeMapDao {
    ServiceConsumeMap findMapToRemove(long serviceId, long consumedServiceId);

    ServiceConsumeMap findNonRemovedMap(long serviceId, long consumedServiceId);

    List<? extends ServiceConsumeMap> findConsumedServices(long serviceId);
}
