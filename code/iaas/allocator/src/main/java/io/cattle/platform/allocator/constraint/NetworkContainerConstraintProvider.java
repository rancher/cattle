package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class NetworkContainerConstraintProvider extends CollocationChecker implements AllocationConstraintsProvider {
    @Inject
    JsonMapper jsonMapper;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        if (!attempt.isInstanceAllocation()) {
            return;
        }

        for (Instance instance : attempt.getInstances()) {
            String networkMode = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_NETWORK_MODE).as(String.class);
            if (NetworkConstants.NETWORK_MODE_CONTAINER.equals(networkMode) && instance.getNetworkContainerId() != null) {
                Long containerId = instance.getNetworkContainerId();
                Set<Long> containerIds = new HashSet<Long>();
                containerIds.add(containerId);

                Map<Long, Set<Long>> hostsToInstances = checkAndGetCollocatedInstanceHosts(containerIds, attempt.getInstances());
                for (Map.Entry<Long, Set<Long>> hostToInstances : hostsToInstances.entrySet()) {
                    constraints.add(new CollocationConstraint(hostToInstances.getKey(), hostToInstances.getValue()));
                }
            }
        }
    }

    @Override
    public boolean isCritical() {
        return false;
    }

}
