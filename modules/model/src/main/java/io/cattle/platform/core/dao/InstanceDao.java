package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

import java.util.List;
import java.util.Map;

public interface InstanceDao {

    List<? extends Instance> getNonRemovedInstanceOn(Long hostId);

    List<? extends Instance> findBadInstances(int count);

    Instance getInstanceByUuidOrExternalId(Long accountId, String uuid, String externalId);

    /**
     * @param instance
     * @return Services related to this instance
     */
    List<? extends Service> findServicesFor(Instance instance);

    List<? extends Instance> listNonRemovedNonStackInstances(Account account);

    List<? extends Instance> findInstanceByServiceName(long accountId, String serviceName);

    List<? extends Instance> findInstanceByServiceName(long accountId, String serviceName, String stackName);

    Map<String, Object> getCacheInstanceData(long instanceId);

    void clearCacheInstanceData(long instanceId);

}
