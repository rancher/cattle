package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;

import java.util.List;
import java.util.Map;

public interface ServiceDao {

    List<? extends Instance> getInstanceByDeploymentUnit(Long id);

    List<VolumeData> getVolumeData(long deploymentUnitId);

    Service getServiceByExternalId(Long accountId, String externalId);

    List<Certificate> getLoadBalancerServiceCertificates(Service lbService);

    Certificate getLoadBalancerServiceDefaultCertificate(Service lbService);

    Map<String, DeploymentUnit> getDeploymentUnits(Service service);

    List<Long> getServiceDeploymentUnitsOnHost(Host host);

    DeploymentUnit createDeploymentUnit(long accountId, Long serviceId,
            long stackId, Long hostId, String serviceIndex, Long revisionId, boolean active);

    Stack getOrCreateDefaultStack(long accountId);

    List<Instance> getInstancesToGarbageCollect(Service service);

    Instance createServiceInstance(Map<String, Object> properties, Long serviceId, Long nextId);

    Long getNextCreate(Long serviceId);

    List<? extends Service> findServiceByName(long accountId, String serviceName);

    List<? extends Service> findServiceByName(long accountId, String serviceName, String stackName);


    List<? extends VolumeTemplate> getVolumeTemplates(Long stackId);

    public class VolumeData {
        public Volume volume;
        public VolumeTemplate template;
    }
}
