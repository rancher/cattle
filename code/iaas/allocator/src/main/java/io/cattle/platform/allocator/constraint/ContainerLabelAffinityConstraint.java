package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;

import java.util.Set;

import com.google.common.collect.Multimap;

public class ContainerLabelAffinityConstraint implements Constraint {
    public static final String ENV_HEADER_AFFINITY_CONTAINER_LABEL = "affinity:";
    public static final String LABEL_HEADER_AFFINITY_CONTAINER_LABEL = "io.rancher.scheduler.affinity:container_label";

    AllocatorDao allocatorDao;

    AffinityOps op;
    String labelKey;
    String labelValue;

    public ContainerLabelAffinityConstraint(AffinityConstraintDefinition def, AllocatorDao allocatorDao) {
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
                Multimap<String, String> labelsForContainerForHost = allocatorDao.getLabelsForContainersForHost(hostId);
                if (!labelsForContainerForHost.get(labelKey).contains(labelValue)) {
                    return false;
                }
            }
            return true;
        } else {
            for (Long hostId : hostIds) {
                Multimap<String, String> labelsForContainerForHost = allocatorDao.getLabelsForContainersForHost(hostId);
                if (labelsForContainerForHost.get(labelKey).contains(labelValue)) {
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
}
