package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Service;

public interface ServiceDao {
    Service getServiceByExternalId(Long accountId, String externalId);
}
