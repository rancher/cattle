package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.constraint.AffinityConstraintDefinition.AffinityOps;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

public class ContainerAffinityConstraint implements Constraint {

    public static final String ENV_HEADER_AFFINITY_CONTAINER = "affinity:container";
    public static final String LABEL_HEADER_AFFINITY_CONTAINER = "io.rancher.scheduler." + ENV_HEADER_AFFINITY_CONTAINER;

    AffinityOps op;
    String containerIdentifier;
    ObjectManager objectManager;
    InstanceDao instanceDao;

    // TODO: Might actually do an early lookup for host lists as an optimization
    public ContainerAffinityConstraint(AffinityConstraintDefinition affinityDef, ObjectManager objectManager, InstanceDao instanceDao) {
        this.op = affinityDef.op;
        this.containerIdentifier = affinityDef.value;
        this.objectManager = objectManager;
        this.instanceDao = instanceDao;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {
        if (candidate.getHost() == null) {
            return false;
        }

        if (op == AffinityOps.SOFT_EQ || op == AffinityOps.EQ) {
            List<? extends Instance> instances = instanceDao.getNonRemovedInstanceOn(candidate.getHost());
            for (Instance instance : instances) {
                if (containerIdentifier != null
                        && (containerIdentifier.equalsIgnoreCase(instance.getName()) || containerIdentifier.equalsIgnoreCase(instance.getUuid()))) {
                    return true;
                }
            }
            return false;
        } else {
            List<? extends Instance> instances = instanceDao.getNonRemovedInstanceOn(candidate.getHost());
            for (Instance instance : instances) {
                if (containerIdentifier != null && (containerIdentifier.equals(instance.getName()) || containerIdentifier.equals(instance.getUuid()))) {
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
        String hard = AffinityOps.EQ.equals(op) || AffinityOps.NE.equals(op) ? "must" : "should";
        String with = AffinityOps.EQ.equals(op) || AffinityOps.SOFT_EQ.equals(op) ? "have" : "not have";
        return String.format("host %s %s a container with name or uuid %s", hard, with, containerIdentifier);
    }
}
