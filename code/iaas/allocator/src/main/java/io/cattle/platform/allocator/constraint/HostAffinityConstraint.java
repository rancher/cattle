package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.constants.CommonStatesConstants;

import java.util.Map;

public class HostAffinityConstraint implements Constraint {
    public static final String ENV_HEADER_AFFINITY_HOST_LABEL = "constraint:";
    public static final String LABEL_HEADER_AFFINITY_HOST_LABEL = "io.rancher.scheduler.affinity:host_label";

    AllocatorDao allocatorDao;

    AffinityOps op;
    String labelKey;
    String labelValue;

    // TODO: Might actually do an early lookup for host lists as an optimization
    public HostAffinityConstraint(AffinityConstraintDefinition def, AllocatorDao allocatorDao) {
        this.op = def.op;
        this.labelKey = def.key;
        this.labelValue = def.value;

        this.allocatorDao = allocatorDao;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        if (candidate.getHost() == null) {
            return false;
        }

        if (op == AffinityOps.SOFT_EQ || op == AffinityOps.EQ) {
            Map<String, String[]> labelsForHost = allocatorDao.getLabelsForHost(candidate.getHost());
            if (labelsForHost.get(labelKey) == null) { // key doesn't exist
                return false;
            }
            String value = labelsForHost.get(labelKey)[0];
            String hostLabelMapState = labelsForHost.get(labelKey)[1];
            if (!labelValue.equals(value) || CommonStatesConstants.REMOVING.equals(hostLabelMapState)) {
                return false;
            }
        } else {
            Map<String, String[]> labelsForHost = allocatorDao.getLabelsForHost(candidate.getHost());
            if (labelsForHost.get(labelKey) != null
                    && labelValue.equals(labelsForHost.get(labelKey)[0])
                    && (CommonStatesConstants.CREATING.equals(labelsForHost.get(labelKey)[1])
                            || CommonStatesConstants.CREATED.equals(labelsForHost.get(labelKey)[1]))) {
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
