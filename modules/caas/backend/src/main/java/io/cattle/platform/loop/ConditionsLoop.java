package io.cattle.platform.loop;

import io.cattle.platform.condition.deployment.DeploymentConditions;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;

import java.util.List;

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
        return Result.DONE;
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
