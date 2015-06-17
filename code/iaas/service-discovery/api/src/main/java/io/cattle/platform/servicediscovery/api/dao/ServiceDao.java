package io.cattle.platform.servicediscovery.api.dao;

import io.cattle.platform.core.model.Service;

import java.util.List;

public interface ServiceDao {

    /**
     * @param hostId
     * @return Collection of Services running on host
     */
    List<? extends Service> getServicesOnHost(long hostId);
}
