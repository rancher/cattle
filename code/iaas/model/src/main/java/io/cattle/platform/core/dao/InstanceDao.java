package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;

import java.util.List;
import java.util.Map;

public interface InstanceDao {

    List<? extends Instance> getNonRemovedInstanceOn(Long hostId);

    List<? extends Instance> findBadInstances(int count);

    List<? extends InstanceHostMap> findBadInstanceHostMaps(int count);

    List<? extends InstanceLink> findBadInstanceLinks(int count);

    List<? extends Nic> findBadNics(int count);

    Instance getInstanceByUuidOrExternalId(Long accountId, String uuid, String externalId);

    /**
     * @param instance
     * @return Services related to this instance
     */
    List<? extends Service> findServicesFor(Instance instance);

    List<? extends Instance> listNonRemovedNonStackInstances(Account account);

    List<? extends Instance> findInstanceByServiceName(long accountId, String serviceName);

    List<? extends Instance> findInstanceByServiceName(long accountId, String serviceName, String stackName);

    List<? extends Host> findHosts(long accountId, long instanceId);

    Map<String, Object> getCacheInstanceData(long instanceId);

    void clearCacheInstanceData(long instanceId);

    List<PublicEndpoint> getPublicEndpoints(long accountId, Long serviceId, Long hostId);

    List<GenericObject> getImagePullTasks(long accountId, List<String> images, Map<String, String> labels);

    void updatePorts(Instance instance, List<String> ports);
}
