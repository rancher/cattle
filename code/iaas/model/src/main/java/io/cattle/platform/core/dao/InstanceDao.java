package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Instance;

import java.util.List;

public interface InstanceDao {

    boolean isOnSharedStorage(Instance instance);

    List<? extends Instance> getNonRemovedInstanceOn(Long hostId);

    Instance getInstanceByUuidOrExternalId(Long accountId, String uuid, String externalId);

}
