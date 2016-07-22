package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Environment;

public interface EnvironmentDao {
    Environment getEnvironmentByExternalId(Long accountId, String externalId);
}
