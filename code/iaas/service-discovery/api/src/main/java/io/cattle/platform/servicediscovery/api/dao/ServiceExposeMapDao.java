package io.cattle.platform.servicediscovery.api.dao;

import io.cattle.platform.core.model.Environment;
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
     * @param instanceName
     * @return
     */
    Pair<Instance, ServiceExposeMap> createServiceInstance(Map<String, Object> properties, Service service,
            String instanceName);

    List<? extends Instance> listServiceInstances(long serviceId);

    /**
     * this method updates service's instances' names based on the environment/service name. Invoked on the
     * service name change
     * 
     * @param service
     */
    void updateServiceName(Service service);

    /**
     * this method updates environment's instances' names based on the environment/service name. Invoked on the
     * environment name change
     * 
     * @param environment
     */
    void updateEnvironmentName(Environment env);

    List<? extends ServiceExposeMap> getNonRemovedServiceInstanceMap(long serviceId);
    
    void updateScale(List<Service> services, Integer scale);

    List<Service> collectSidekickServices(Service initialService, Map<String, String> initialSvcLabels);

    ServiceExposeMap findInstanceExposeMap(Instance instance);

    ServiceExposeMap createServiceInstanceMap(Service service, Instance instance);

    ServiceExposeMap createIpToServiceMap(Service service, String ipAddress);

    ServiceExposeMap getServiceIpExposeMap(Service service, String ipAddress);

    List<? extends Service> getActiveServices(long accountId);

    List<? extends ServiceExposeMap> getNonRemovedServiceIpMaps(long serviceId);
}
