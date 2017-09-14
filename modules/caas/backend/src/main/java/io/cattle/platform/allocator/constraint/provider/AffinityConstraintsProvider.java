package io.cattle.platform.allocator.constraint.provider;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.util.DataAccessor;

import java.util.List;
import java.util.Map;

/**
 * Future optimization: For hard constraints, we might be able to update the DB query to do
 * the lookup.
 *
 */
public class AffinityConstraintsProvider implements AllocationConstraintsProvider {

    AllocationHelper allocationHelper;

    public AffinityConstraintsProvider(AllocationHelper allocationHelper) {
        super();
        this.allocationHelper = allocationHelper;
    }

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        for (Instance instance : attempt.getInstances()) {
            Map<String, ?> env = DataAccessor.fieldMapRO(instance, InstanceConstants.FIELD_ENVIRONMENT);
            List<Constraint> affinityConstraintsFromEnv = allocationHelper.extractConstraintsFromEnv(env);
            constraints.addAll(affinityConstraintsFromEnv);

            // Currently, intentionally duplicating code to be explicit
            Map<String, ?> labels = DataAccessor.fieldMapRO(instance, InstanceConstants.FIELD_LABELS);
            List<Constraint> affinityConstraintsFromLabels = allocationHelper.extractConstraintsFromLabels(labels, instance);
            constraints.addAll(affinityConstraintsFromLabels);
        }
    }

    @Override
    public boolean isCritical() {
        return false;
    }
}
