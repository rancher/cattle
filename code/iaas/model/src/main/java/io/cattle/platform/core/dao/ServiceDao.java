package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;

public interface ServiceDao {
    Service getServiceByExternalId(Long accountId, String externalId);

    ServiceIndex createServiceIndex(Service service, String launchConfigName, String serviceIndex);
}
