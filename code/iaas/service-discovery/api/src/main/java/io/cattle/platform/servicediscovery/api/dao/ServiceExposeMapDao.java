package io.cattle.platform.servicediscovery.api.dao;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;

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
    Pair<Instance, ServiceExposeMap> createServiceInstance(Map<String, Object> properties, Service service);

    List<? extends Instance> listServiceManagedInstances(long serviceId);

    ServiceExposeMap findInstanceExposeMap(Instance instance);

    ServiceExposeMap createServiceInstanceMap(Service service, Instance instance, boolean managed);

    ServiceExposeMap createIpToServiceMap(Service service, String ipAddress);

    ServiceExposeMap getServiceIpExposeMap(Service service, String ipAddress);

    List<? extends Service> getActiveServices(long accountId);

    List<? extends ServiceExposeMap> getNonRemovedServiceIpMaps(long serviceId);

    List<? extends ServiceExposeMap> getNonRemovedServiceInstanceMaps(long serviceId);

    List<? extends ServiceExposeMap> getNonRemovedServiceHostnameMaps(long serviceId);

    List<? extends ServiceExposeMap> getUnmanagedServiceInstanceMapsToRemove(long serviceId);

    Host getHostForInstance(long instanceId);

    boolean isActiveMap(ServiceExposeMap serviceExposeMap);

    ServiceExposeMap getServiceHostnameExposeMap(Service service, String hostName);

    ServiceExposeMap createHostnameToServiceMap(Service service, String hostName);

    Service getIpAddressService(String ipAddress, long accountId);

    ServiceExposeMap getServiceInstanceMap(Service service, Instance instance);

    List<? extends ServiceExposeMap> getInstancesSetForUpgrade(long serviceId);

    List<? extends Instance> getInstancesToUpgrade(Service service, String launchConfigName, String toVersion);

    List<? extends Instance> getInstancesToCleanup(Service service, String launchConfigName, String toVersion);

    List<? extends Instance> getUpgradedInstances(Service service, String launchConfigName, String toVersion, boolean managed);

}
