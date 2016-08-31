package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class CollocationConstraint extends HardConstraint implements Constraint {

    GenericMapDao mapDao;

    Collection<Integer> otherInstances;

    public CollocationConstraint(Collection<Integer> otherInstances, GenericMapDao mapDao) {
        this.otherInstances = otherInstances;
        this.mapDao = mapDao;
    }

    @Override
    public boolean matches(AllocationAttempt attempt,
            AllocationCandidate candidate) {
        for (Integer instanceId : otherInstances) {
            List<? extends InstanceHostMap> maps = mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instanceId);
            if (maps.size() > 0) {
                Long hostId = maps.get(0).getHostId();
                if (hostId == null) {
                    throw new FailedToAllocate("Dependent instance not allocated yet: " + instanceId);
                }
                if (!hostId.equals(candidate.getHost())) {
                    return false;
                }
            } else {
                throw new FailedToAllocate("Dependent instance not allocated yet: " + instanceId);
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("On the same host as " + StringUtils.join(otherInstances, ", "));
    }

}
