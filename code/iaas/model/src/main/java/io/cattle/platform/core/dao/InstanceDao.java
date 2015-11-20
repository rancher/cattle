package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

import java.util.List;

public interface InstanceDao {

    boolean isOnSharedStorage(Instance instance);

    List<? extends Instance> getNonRemovedInstanceOn(Long hostId);

    Instance getInstanceByUuidOrExternalId(Long accountId, String uuid, String externalId);

    /**
     * @param instance
     * @return Services related to this instance
     */
    List<? extends Service> findServicesFor(Instance instance);

    List<? extends Instance> listNonRemovedInstances(Account account, boolean forService);

    List<? extends Instance> findInstancesFor(Service service);

    Long getInstanceHostId(long instanceId);

    Service getServiceManaging(long instanceId);

}
