package io.cattle.platform.servicediscovery.api.dao;

import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;

import java.util.List;
import java.util.Map;

public interface ServiceExposeMapDao {

    List<? extends Instance> listActiveServiceInstances(long serviceId);

    Instance getActiveServiceInstance(long serviceId, String instanceName);

    /**
     * this method is wrapped up in transaction. All instances will get created and scheduled for create inside one
     * transaction
     * 
     * @param properties
     * @param service
     * @param instanceName
     * @return
     */
    Instance createServiceInstance(Map<Object, Object> properties, Service service, String instanceName);

    List<? extends Instance> listServiceInstances(long serviceId);

    /**
     * Lists maps for service instances that are in removed state
     * 
     * @param serviceId
     * @return
     */
    List<? extends ServiceExposeMap> listServiceRemovedInstancesMaps(long serviceId);

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

}
