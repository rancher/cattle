package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.object.ObjectManager;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

public class VolumeAccessModeConstraintProvider implements AllocationConstraintsProvider {

    static final List<Object> IHM_STATES = Arrays.asList(new Object[] { CommonStatesConstants.INACTIVE, CommonStatesConstants.DEACTIVATING,
            CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING });

    @Inject
    AllocatorDao allocatorDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        for (Instance instance : attempt.getInstances()) {
            List<Volume> volumes = InstanceHelpers.extractVolumesFromMounts(instance, objectManager);
            for (Volume v : volumes) {
                if (VolumeConstants.ACCESS_MODE_SINGLE_HOST_RW.equals(v.getAccessMode())) {
                    if (v.getHostId() != null) {
                        boolean hardConstraint = allocatorDao.isVolumeInUseOnHost(v.getId(), v.getHostId());
                        constraints.add(new VolumeAccessModeSingleHostConstraint(v.getHostId(), v.getId(), hardConstraint));
                    }
                } else if (VolumeConstants.ACCESS_MODE_SINGLE_INSTANCE_RW.equals(v.getAccessMode())) {
                    List<Long> currentlyUsedBy = allocatorDao.getInstancesWithVolumeMounted(v.getId(), instance.getId());
                    if (currentlyUsedBy.size() > 0) {
                        constraints.add(new VolumeAccessModeSingleInstanceConstraint(v.getId(), v.getAccessMode(), currentlyUsedBy));
                    }
                }
            }
        }
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}
