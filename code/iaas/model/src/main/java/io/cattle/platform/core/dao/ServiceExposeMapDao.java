package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.records.ServiceRecord;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public interface ServiceExposeMapDao {

    /**
     * this method is wrapped up in transaction. All instances will get created and scheduled for create inside one
     * transaction
     *
     * @param properties
     * @param service
     * @return
     */
    Pair<Instance, ServiceExposeMap> createServiceInstance(Map<String, Object> properties,
            Service service, ServiceRecord record);

    List<? extends Instance> listServiceManagedInstances(Service service);

    List<? extends Instance> listServiceManagedInstances(Service service, String launchConfigName);

    ServiceExposeMap createServiceInstanceMap(Service service, Instance instance, boolean managed);

    ServiceExposeMap getServiceIpExposeMap(Service service, String ipAddress);

    List<? extends Service> getActiveServices(long accountId);

    List<? extends ServiceExposeMap> getUnmanagedServiceInstanceMapsToRemove(long serviceId);

    Host getHostForInstance(long instanceId);

    List<? extends Instance> getInstancesSetForUpgrade(long serviceId);

    List<? extends Instance> getInstancesToUpgrade(Service service, String launchConfigName, String toVersion);

    List<? extends Instance> getInstancesToCleanup(Service service, String launchConfigName, String toVersion);

    List<? extends Instance> getUpgradedUnmanagedInstances(Service service, String launchConfigName, String toVersion);

    Integer getCurrentScale(long serviceId);

    List<? extends Instance> listServiceManagedInstancesAll(Service service);

    List<Pair<Instance, ServiceExposeMap>> listDeploymentUnitInstancesExposeMaps(Service service, DeploymentUnit unit);
}
