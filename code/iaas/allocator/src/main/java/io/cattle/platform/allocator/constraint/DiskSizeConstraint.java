package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.service.DiskInfo;
import io.cattle.platform.allocator.util.AllocatorUtils;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class DiskSizeConstraint extends HardConstraint implements Constraint {

    private ObjectManager objectManager;
    private Instance instance;

    public DiskSizeConstraint(Instance instance, ObjectManager objMgr) {
        this.instance = instance;
        this.objectManager = objMgr;
    }

    @Override
    public boolean matches(AllocationCandidate candidate) {

        // if no disk with big enough free space for this host, then candidate is no good
        Map<Pair<String, Long>, DiskInfo> volumeToDiskMapping = AllocatorUtils.allocateDiskForVolumes(candidate.getHost(), instance, this.objectManager);
        return volumeToDiskMapping != null;
    }

    @Override
    public String toString() {
        return String.format("host needs more free disk space");
    }

}
