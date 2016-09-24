package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ServiceDao {
    Service getServiceByExternalId(Long accountId, String externalId);

    ServiceIndex createServiceIndex(Service service, String launchConfigName, String serviceIndex);

    Service getServiceByServiceIndexId(long serviceIndexId);

    boolean isServiceInstance(Instance instance);

    Map<Long, List<Object>> getServicesForInstances(List<Long> ids, IdFormatter idFormatter);

    Map<Long, List<Object>> getInstances(List<Long> ids, IdFormatter idFormatter);

    Map<Long, ServiceMapping> getServicesMappings(List<Long> ids, IdFormatter idFormatter);

    class ServiceMapping {
        public List<Object> consumed = new ArrayList<>();
        public List<Object> consumedBy = new ArrayList<>();
    }
}
