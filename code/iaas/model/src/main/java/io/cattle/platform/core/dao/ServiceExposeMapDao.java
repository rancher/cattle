package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public interface ServiceExposeMapDao {

    List<? extends Instance> listServiceManagedInstances(Service service);

    ServiceExposeMap createServiceInstanceMap(Service service, Instance instance, boolean managed);

    List<? extends Service> getActiveServices(long accountId);

    List<? extends ServiceExposeMap> getUnmanagedServiceInstanceMapsToRemove(long serviceId);

    List<? extends Instance> getServiceInstancesSetForUpgrade(long serviceId);

    Integer getCurrentScale(long serviceId);

    List<? extends Instance> listServiceManagedInstancesAll(Service service);

    List<Pair<Instance, ServiceExposeMap>> listDeploymentUnitInstances(Service service, DeploymentUnit unit, boolean forCurrentRevision);

    ServiceExposeMap createServiceInstanceMap(Service service, Instance instance, boolean managed, String dnsPrefix);

    List<? extends Instance> getDeploymentUnitInstancesSetForUpgrade(DeploymentUnit unit);

}
