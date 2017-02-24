package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.dao.impl.InstanceDaoImpl.IpAddressToServiceIndex;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.InstanceRevision;
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

    List<? extends Instance> findUnallocatedInstanceByDeploymentUnitUuid(long accountId, String deploymentUnitUuid);

    List<? extends Host> findHosts(long accountId, long instanceId);

    Map<String, Object> getCacheInstanceData(long instanceId);

    void clearCacheInstanceData(long instanceId);

    List<IpAddressToServiceIndex> getIpToIndex(Service service);

    List<PublicEndpoint> getPublicEndpoints(long accountId, Long serviceId, Long hostId);

    List<? extends Service> findServicesNonRemovedLinksOnly(Instance instance);

    InstanceRevision createRevision(Instance instance, Map<String, Object> spec);

    void cleanupInstanceRevisions(Instance instance);

    Map<String, Object> getInstanceSpec(Instance instance);

}
