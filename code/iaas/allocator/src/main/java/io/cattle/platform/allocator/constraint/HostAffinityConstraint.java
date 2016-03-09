package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.constants.CommonStatesConstants;

import java.util.Map;
import java.util.Set;

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
    public boolean matches(AllocationAttempt attempt,
            AllocationCandidate candidate) {
        Set<Long> hostIds = candidate.getHosts();
        if (op == AffinityOps.SOFT_EQ || op == AffinityOps.EQ) {
            for (Long hostId : hostIds) {
                Map<String, String[]> labelsForHost = allocatorDao.getLabelsForHost(hostId);
                if (labelsForHost.get(labelKey) == null) { // key doesn't exist
                    return false;
                }
                String value = labelsForHost.get(labelKey)[0];
                String hostLabelMapState = labelsForHost.get(labelKey)[1];
                if (!labelValue.equals(value)) { // value doesn't match
                    return false;
                }
                if (CommonStatesConstants.REMOVING.equals(hostLabelMapState)) { // value matches but it's currently getting removed
                    return false;
                }
            }
            return true;
        } else {
            for (Long hostId : hostIds) {
                Map<String, String[]> labelsForHost = allocatorDao.getLabelsForHost(hostId);
                if (labelsForHost.get(labelKey) != null 
                        && labelValue.equals(labelsForHost.get(labelKey)[0]) 
                        && (CommonStatesConstants.CREATING.equals(labelsForHost.get(labelKey)[1])
                                || CommonStatesConstants.CREATED.equals(labelsForHost.get(labelKey)[1]))) {
                    return false;
                }
            }
            return true;
        }
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
