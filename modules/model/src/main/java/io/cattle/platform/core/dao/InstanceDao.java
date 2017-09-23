package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Instance;

import java.util.List;

public interface InstanceDao {

    List<? extends Instance> getNonRemovedInstanceOn(Long hostId);

    List<? extends Instance> getOtherDeploymentInstances(Instance instance);

}
