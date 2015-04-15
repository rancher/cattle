package io.cattle.platform.servicediscovery.api.dao;

import io.cattle.platform.core.model.Instance;

import java.util.List;

public interface ServiceExposeMapDao {

    List<? extends Instance> listNonRemovedInstancesForService(long serviceId);

    Instance getServiceInstance(long serviceId, String instanceName);

}
