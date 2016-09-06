package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.constants.DockerNetworkConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class NetworkContainerConstraintProvider implements AllocationConstraintsProvider {
    @Inject
    GenericMapDao mapDao;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        if (!attempt.isInstanceAllocation()) {
            return;
        }

        for (Instance instance : attempt.getInstances()) {
            String networkMode = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_NETWORK_MODE).as(String.class);
            if (DockerNetworkConstants.NETWORK_MODE_CONTAINER.equals(networkMode) && instance.getNetworkContainerId() != null) {
                Integer containerId = instance.getNetworkContainerId().intValue();
                Set<Integer> containerIds = new HashSet<Integer>();
                containerIds.add(containerId);
                constraints.add(new CollocationConstraint(containerIds, mapDao));
            }
        }
    }

    @Override
    public boolean isCritical() {
        return false;
    }

}
