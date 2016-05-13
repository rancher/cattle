package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.service.DiskInfo;
import io.cattle.platform.allocator.util.AllocatorUtils;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskSizeConstraint extends HardConstraint implements Constraint {

    private static final Logger log = LoggerFactory.getLogger(DiskSizeConstraint.class);

    private ObjectManager objectManager;

    public DiskSizeConstraint(ObjectManager objMgr) {
        this.objectManager = objMgr;
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        Instance instance = attempt.getInstance();
        if (instance == null) {
            return false;
        }
        Set<Long> hostIds = candidate.getHosts();

        // if one of the host does not have enough free space then return false
        for (Long hostId : hostIds) {
            Map<Pair<String, Long>, DiskInfo> volumeToDiskMapping = AllocatorUtils.allocateDiskForVolumes(hostId,
                    instance, this.objectManager);

            // if no disk with big enough free space for this host, then
            // candidate is no good
            if (volumeToDiskMapping == null) {
                log.debug("Scheduling instance [{}] to host [{}] rejected", attempt.getInstanceId(), hostId);
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("host needs more free disk space");
    }

}
