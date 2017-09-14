package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;

import java.util.List;
import java.util.Set;

public interface InstanceDao {

    List<? extends Instance> getNonRemovedInstanceOn(Long hostId);

    Instance getInstanceByUuidOrExternalId(Long clusterId, String uuid, String externalId);

    List<? extends Instance> getOtherDeploymentInstances(Instance instance);

    List<? extends Credential> getCredentials(Set<Long> credentialIds);

}
