package io.cattle.platform.servicediscovery.api.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

import java.util.List;

public interface ServiceDao {

    /**
     * @param instance
     * @return Services related to this instance
     */
    List<? extends Service> findServicesFor(Instance instance);

}
