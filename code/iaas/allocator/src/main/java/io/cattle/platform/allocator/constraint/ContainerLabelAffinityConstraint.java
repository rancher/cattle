package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.resource.service.EnvironmentResourceManager;

public class ContainerLabelAffinityConstraint implements Constraint {
    public static final String ENV_HEADER_AFFINITY_CONTAINER_LABEL = "affinity:";
    public static final String LABEL_HEADER_AFFINITY_CONTAINER_LABEL = "io.rancher.scheduler.affinity:container_label";

    EnvironmentResourceManager envResourceManager;

    AffinityOps op;
    String labelKey;
    String labelValue;

    public ContainerLabelAffinityConstraint(AffinityConstraintDefinition def, EnvironmentResourceManager envResourceManager) {
        this.op = def.op;
        this.labelKey = def.key;
        this.labelValue = def.value;

        this.envResourceManager = envResourceManager;
    }

    // If necessary we can do additional optimizations to allow multiple container label or host label
    // affinity constraints to share results from DB queries
    @Override
    public boolean matches(AllocationCandidate candidate) {
        if (candidate.getHost() == null) {
            return false;
        }

        if (op == AffinityOps.SOFT_EQ || op == AffinityOps.EQ) {
            return envResourceManager.hostHasContainerLabel(candidate.getAccountId(), candidate.getHost(), labelKey, labelValue);
        } else {
            // Anti-affinity
            return !envResourceManager.hostHasContainerLabel(candidate.getAccountId(), candidate.getHost(), labelKey, labelValue);
        }
    }

    @Override
    public boolean isHardConstraint() {
        return (op == AffinityOps.EQ || op == AffinityOps.NE);
    }

    @Override
    public String toString() {
        String hard = AffinityOps.EQ.equals(op) || AffinityOps.NE.equals(op) ? "must" : "should";
        String with = AffinityOps.EQ.equals(op) || AffinityOps.SOFT_EQ.equals(op) ? "have" : "not have";
        return String.format("host %s %s a container with label %s=%s", hard, with, labelKey, labelValue);
    }

}
