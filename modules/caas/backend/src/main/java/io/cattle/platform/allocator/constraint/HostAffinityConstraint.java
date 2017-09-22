package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.service.AllocationHelper;

import java.util.Map;

public class HostAffinityConstraint implements Constraint {
    public static final String ENV_HEADER_AFFINITY_HOST_LABEL = "constraint:";
    public static final String LABEL_HEADER_AFFINITY_HOST_LABEL = "io.rancher.scheduler.affinity:host_label";

    AffinityOps op;
    String labelKey;
    String labelValue;
    AllocationHelper allocationHelper;

    public HostAffinityConstraint(AffinityConstraintDefinition def, AllocationHelper allocationHelper) {
        this.op = def.op;
        this.labelKey = def.key;
        this.labelValue = def.value;
        this.allocationHelper = allocationHelper;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        if (candidate.getHost() == null) {
            return false;
        }

        if (op == AffinityOps.SOFT_EQ || op == AffinityOps.EQ) {
            Map<String, String> labelsForHost = allocationHelper.getLabelsForHost(candidate.getClusterId(), candidate.getHostUuid());
            String value = labelsForHost.get(labelKey);
            if (value == null) { // key doesn't exist
                return false;
            }
            if (!labelValue.equals(value)) {
                return false;
            }
        } else {
            Map<String, String> labelsForHost = allocationHelper.getLabelsForHost(candidate.getClusterId(), candidate.getHostUuid());
            if (labelValue.equals(labelsForHost.get(labelKey))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isHardConstraint() {
        return (op == AffinityOps.EQ || op == AffinityOps.NE);
    }

    @Override
    public String toString() {
        return String.format("needs host with label %s%s: %s", labelKey, op.getLabelSymbol(), labelValue);
    }
}
