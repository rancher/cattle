package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;

import java.util.List;
import java.util.Set;

public interface InstanceDao {

    List<? extends Instance> getNonRemovedInstanceOn(Long hostId);

    Instance getInstanceByUuidOrExternalId(Long accountId, String uuid, String externalId);

    /**
     * @param instance
     * @return Services related to this instance
     */
    List<? extends Instance> listNonRemovedNonStackInstances(Account account);

    List<? extends Instance> getOtherDeploymentInstances(Instance instance);

    List<? extends Credential> getCredentials(Set<Long> credentialIds);

}
