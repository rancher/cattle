package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;
import java.util.Map;

public interface ServiceDao {
    Service getServiceByExternalId(Long accountId, String externalId);

    ServiceIndex createServiceIndex(Service service, String launchConfigName, String serviceIndex);

    Service getServiceByServiceIndexId(long serviceIndexId);

    boolean isServiceInstance(Instance instance);

    Map<Long, List<Object>> getServicesForInstances(List<Long> ids, IdFormatter idFormatter);

    Map<Long, List<Object>> getInstances(List<Long> ids, IdFormatter idFormatter);

    Map<Long, List<ServiceLink>> getServiceLinks(List<Long> ids);

    class ServiceLink {
        public String linkName;
        public String serviceName;
        public Long serviceId;
        public Long stackId;
        public String stackName;

        public ServiceLink(String linkName, String serviceName, Long serviceId, Long stackId, String stackName) {
            super();
            this.linkName = linkName;
            this.serviceName = serviceName;
            this.serviceId = serviceId;
            this.stackId = stackId;
            this.stackName = stackName;
        }
    }

    List<Certificate> getLoadBalancerServiceCertificates(Service lbService);

    Certificate getLoadBalancerServiceDefaultCertificate(Service lbService);

    HealthcheckInstanceHostMap getHealthCheckInstanceUUID(String hostUUID, String instanceUUID);
}
