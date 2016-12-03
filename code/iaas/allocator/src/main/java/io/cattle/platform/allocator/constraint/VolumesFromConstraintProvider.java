package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class VolumesFromConstraintProvider extends CollocationChecker implements AllocationConstraintsProvider {
    @Inject
    JsonMapper jsonMapper;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        for (Instance instance : attempt.getInstances()) {
            @SuppressWarnings("unchecked")
            Set<Integer> intDataVolumesFrom = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_VOLUMES_FROM).as(jsonMapper, Set.class);
            if (intDataVolumesFrom != null && !intDataVolumesFrom.isEmpty()) {
                Set<Long> dataVolumesFrom = new HashSet<>();
                for (Integer i : intDataVolumesFrom) {
                    dataVolumesFrom.add(i.longValue());
                }

                Map<Long, Set<Long>> hostsToInstances = checkAndGetCollocatedInstanceHosts(dataVolumesFrom, attempt.getInstances());
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
