package io.cattle.platform.allocator.constraint.provider;

import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.VolumeAccessModeSingleHostConstraint;
import io.cattle.platform.allocator.constraint.VolumeAccessModeSingleInstanceConstraint;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationLog;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class VolumeAccessModeConstraintProvider implements AllocationConstraintsProvider {

    static final List<Object> IHM_STATES = Arrays.asList(new Object[] { CommonStatesConstants.INACTIVE, CommonStatesConstants.DEACTIVATING,
            CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING });

    @Inject
    AllocatorDao allocatorDao;

    @Inject
    ObjectManager objectManager;
    
    @Inject
    IdFormatter idFormatter;

    @Override
    public void appendConstraints(AllocationAttempt attempt, AllocationLog log, List<Constraint> constraints) {
        for (Instance instance : attempt.getInstances()) {
            List<Volume> volumes = InstanceHelpers.extractVolumesFromMounts(instance, objectManager);
            for (Volume v : volumes) {
                if (VolumeConstants.ACCESS_MODE_SINGLE_HOST_RW.equals(v.getAccessMode())) {
                    Long hostID = null;
                    Object hostIdObject = DataAccessor.fromDataFieldOf(v).withKey(VolumeConstants.FIELD_LAST_ALLOCATED_HOST_ID).get();
                    if ( hostIdObject != null) {
                        hostID = Long.parseLong(hostIdObject.toString());
                    }
                    Set<Long> hostIds = allocatorDao.findHostsWithVolumeInUse(v.getId());
                    boolean hardConstraint = false;
                    if (hostIds.size() == 0) {
                        hardConstraint = false;
                    } else if (hostIds.size() == 1) {
                        hardConstraint = true;
                        for(Long id: hostIds) {
                            hostID = id;
                        }
                    } else {
                        throw new FailedToAllocate("SingleHostRW volume is used by mutiple hosts");
                    }
                    if (hostID != null) {
                        Host host = objectManager.loadResource(Host.class, hostID);
                        String hostName = DataAccessor.fieldString(host, HostConstants.FIELD_HOSTNAME);
                        String hID = idFormatter.formatId(HostConstants.TYPE, hostID).toString();
                        String vID = idFormatter.formatId(VolumeConstants.TYPE, v.getId()).toString();
                        constraints.add(new VolumeAccessModeSingleHostConstraint(hID, vID, v.getName(), hostName, hardConstraint));
                    }
                } else if (VolumeConstants.ACCESS_MODE_SINGLE_INSTANCE_RW.equals(v.getAccessMode())) {
                    List<Long> currentlyUsedBy = allocatorDao.getInstancesWithVolumeMounted(v.getId(), instance.getId());
                    if (currentlyUsedBy.size() > 0) {
                        String vID = idFormatter.formatId(VolumeConstants.TYPE, v.getId()).toString();
                        constraints.add(new VolumeAccessModeSingleInstanceConstraint(v.getName(), vID, v.getAccessMode(), currentlyUsedBy));
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
