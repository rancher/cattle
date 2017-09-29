package io.cattle.platform.loop;

import io.cattle.platform.condition.deployment.DeploymentConditions;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.addon.metadata.ServiceInfo;
import io.cattle.platform.core.addon.metadata.StackInfo;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;
import java.util.Map;

public class ConditionsLoop implements Loop {

    long accountId;
    MetadataManager metadataManager;
    DeploymentConditions deploymentConditions;

    public ConditionsLoop(long accountId, MetadataManager metadataManager, DeploymentConditions deploymentConditions) {
        this.accountId = accountId;
        this.metadataManager = metadataManager;
        this.deploymentConditions = deploymentConditions;
    }

    @Override
    public Result run(List<Object> input) {
        checkHealthyHosts();
        checkServiceDependencies();
        return Result.DONE;
    }

    private void checkServiceDependencies() {
        Metadata metadata = metadataManager.getMetadataForAccount(accountId);
        Map<Long, StackInfo> stacks = CollectionUtils.mapBy(metadata.getStacks(), StackInfo::getId);
        Map<Long, ServiceInfo> services = CollectionUtils.mapBy(metadata.getServices(), ServiceInfo::getId);

        for (InstanceInfo instanceInfo : metadata.getInstances()) {
            ServiceInfo service = services.get(instanceInfo.getServiceId());
            if (service == null || !service.isGlobal()) {
                // Don't care about these instances, only standalone and global
                continue;
            }

            StackInfo stack = stacks.get(instanceInfo.getStackId());
            deploymentConditions.serviceDependency.setState(stack, service, instanceInfo);
        }

        for (ServiceInfo serviceInfo : services.values()) {
            StackInfo stack = stacks.get(serviceInfo.getStackId());
            deploymentConditions.serviceDependency.setState(stack, serviceInfo, null);
        }

    }

    private void checkHealthyHosts() {
        Metadata metadata = metadataManager.getMetadataForAccount(accountId);
        if (!metadata.isClusterOwner()) {
            return;
        }

        boolean oneGood = false;
        for (HostInfo host : metadata.getHosts()) {
            if (CommonStatesConstants.ACTIVE.equalsIgnoreCase(host.getState())
                    && CommonStatesConstants.ACTIVE.equalsIgnoreCase(host.getAgentState())) {
                oneGood = true;
                deploymentConditions.healthHosts.setHostHealth(host.getId(), true);
            } else {
                deploymentConditions.healthHosts.setHostHealth(host.getId(), false);
            }
        }

        deploymentConditions.healthHosts.setClusterHealth(metadata.getClusterId(), oneGood);
    }

}
