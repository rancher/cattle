package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public interface ServiceDao {

    List<InstanceData> getInstanceData(Long id, String uuid);

    List<VolumeData> getVolumeData(long deploymentUnitId);

    Service getServiceByExternalId(Long accountId, String externalId);

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

    Map<String, DeploymentUnit> getDeploymentUnits(Service service);

    List<Long> getServiceDeploymentUnitsOnHost(Host host);

    DeploymentUnit createDeploymentUnit(long accountId, Long serviceId,
            long stackId, Long hostId, String serviceIndex, Long revisionId, boolean active);

    Stack getOrCreateDefaultStack(long accountId);

    List<Instance> getInstancesToGarbageCollect(Service service);

    Pair<Instance, ServiceExposeMap> createServiceInstance(Map<String, Object> properties, Long serviceId, Long nextId);

    Long getNextCreate(Long serviceId);

    List<? extends VolumeTemplate> getVolumeTemplates(Long stackId);

    public class InstanceData {
        public Instance instance;
        public ServiceExposeMap serviceExposeMap;
    }

    public class VolumeData {
        public Volume volume;
        public VolumeTemplate template;
    }
}
