package io.cattle.platform.loop;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.metadata.model.InstanceInfo;
import io.cattle.platform.metadata.model.ServiceInfo;
import io.cattle.platform.metadata.service.Metadata;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.*;

public class ServiceMembershipLoop implements Loop {

    EnvironmentResourceManager envResourceManager;
    long accountId;
    ObjectManager objectManager;

    public ServiceMembershipLoop(EnvironmentResourceManager envResourceManager, long accountId, ObjectManager objectManager) {
        super();
        this.envResourceManager = envResourceManager;
        this.accountId = accountId;
        this.objectManager = objectManager;
    }

    @Override
    public Result run(Object input) {
        Metadata metadata = envResourceManager.getMetadata(accountId);
        Map<Long, Set<Long>> serviceToInstances = new HashMap<>();
        Map<String, List<ServiceInfo>> selectorServices = metadata.getServices().stream()
                .filter((service) -> StringUtils.isNotBlank(service.getSelector()))
                .collect(groupingBy((service) -> service.getSelector()));

        for (InstanceInfo instanceInfo : metadata.getInstances()) {
            Set<Long> serviceIds = new HashSet<>();

            if (instanceInfo.getServiceId() != null) {
                serviceIds.add(instanceInfo.getServiceId());
            }

            for (Map.Entry<String, List<ServiceInfo>> entry : selectorServices.entrySet()) {
                if (SelectorUtils.isSelectorMatch(entry.getKey(), instanceInfo.getLabels())) {
                    entry.getValue().forEach((x) -> serviceIds.add(x.getId()));
                }
            }

            for (Long serviceId : serviceIds) {
                addInstanceToService(serviceToInstances, instanceInfo, serviceId);
            }

            if (!instanceInfo.getServiceIds().equals(serviceIds)) {
                metadata.modify(Instance.class, instanceInfo.getId(), (instance) -> {
                    return objectManager.setFields(instance, InstanceConstants.FIELD_SERVICE_IDS, serviceIds);
                });
            }
        }

        for (ServiceInfo serviceInfo : metadata.getServices()) {
            Set<Long> instanceIds = serviceToInstances.get(serviceInfo.getId());
            if (instanceIds == null) {
                instanceIds = Collections.emptySet();
            }

            if (!instanceIds.equals(serviceInfo.getInstances())) {
                Set<Long> toSetInstanceIds = instanceIds;
                metadata.modify(Service.class, serviceInfo.getId(), (service) -> {
                    return objectManager.setFields(service, ServiceConstants.FIELD_INSTANCE_IDS, toSetInstanceIds);
                });
            }
        }

        return Result.DONE;
    }

    protected void addInstanceToService(Map<Long, Set<Long>> serviceToInstances, InstanceInfo instance, long serviceId) {
        Set<Long> instanceIds = serviceToInstances.get(serviceId);
        if (instanceIds == null) {
            instanceIds = new HashSet<>();
            serviceToInstances.put(serviceId, instanceIds);
        }
        instanceIds.add(instance.getId());
    }

}
