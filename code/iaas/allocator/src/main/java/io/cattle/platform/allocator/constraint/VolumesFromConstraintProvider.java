package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class VolumesFromConstraintProvider implements AllocationConstraintsProvider {

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
            @SuppressWarnings("unchecked")
            Set<Integer> dataVolumesFrom = DataAccessor.fields(instance).withKey(DockerInstanceConstants.FIELD_VOLUMES_FROM).as(jsonMapper, Set.class);
            if (dataVolumesFrom != null && !dataVolumesFrom.isEmpty()) {
                constraints.add(new CollocationConstraint(dataVolumesFrom, mapDao));
            }
        }
    }

    @Override
    public boolean isCritical() {
        return false;
    }
}
