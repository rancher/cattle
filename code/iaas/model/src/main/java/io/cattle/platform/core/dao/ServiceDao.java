package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;
import java.util.Map;

public interface ServiceDao {
    Service getServiceByExternalId(Long accountId, String externalId);

    ServiceIndex createServiceIndex(Service service, String launchConfigName, String serviceIndex);

    Service getServiceByServiceIndexId(long serviceIndexId);

    boolean isServiceManagedInstance(Instance instance);

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

    Map<Long, List<HealthcheckState>> getHealthcheckStatesForInstances(List<Long> ids, IdFormatter idFormatter);

    List<? extends HealthcheckInstance> findBadHealthcheckInstance(int limit);

    List<? extends Service> getSkipServices(long accountId);

    Map<String, DeploymentUnit> getDeploymentUnits(Service service);

    List<? extends Service> getServicesOnHost(long hostId);

    List<? extends Instance> getInstancesWithHealtcheckEnabled(long accountId);

    public Map<Long, List<Instance>> getServiceInstancesWithNoDeploymentUnit();

    List<? extends DeploymentUnit> getUnitsOnHost(Host host, boolean transitioningOnly);

    DeploymentUnit createDeploymentUnit(long accountId, long serviceId,
            long stackId, Map<String, String> labels, Integer serviceIndex);

    Stack getOrCreateDefaultStack(long accountId);

    List<Instance> getInstancesToGarbageCollect(Service service);

}
