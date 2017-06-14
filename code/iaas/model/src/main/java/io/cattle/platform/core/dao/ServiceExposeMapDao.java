package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;

import java.util.List;

public interface ServiceExposeMapDao {

    List<? extends Instance> listServiceManagedInstances(Service service);

    ServiceExposeMap createServiceInstanceMap(Service service, Instance instance, boolean managed);

    List<? extends ServiceExposeMap> getUnmanagedServiceInstanceMapsToRemove(long serviceId);

    ServiceExposeMap createServiceInstanceMap(Service service, Instance instance, boolean managed, String dnsPrefix);

}
