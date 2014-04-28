package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Instance;

public interface InstanceDao {

    boolean isOnSharedStorage(Instance instance);

}
