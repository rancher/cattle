package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.service.CacheManager;
import io.cattle.platform.allocator.service.DiskInfo;
import io.cattle.platform.allocator.service.InstanceDiskReserveInfo;
import io.cattle.platform.allocator.service.HostInfo;
import io.cattle.platform.allocator.service.InstanceInfo;
import io.cattle.platform.object.ObjectManager;

import java.util.Map.Entry;
import java.util.Set;

public class DiskSizeConstraint extends HardConstraint implements Constraint {

    private Long reserveSize;
    private String volumeName;
    private ObjectManager objectManager;

    public DiskSizeConstraint(String volumeName, String sizeWithUnit, ObjectManager objMgr) {
        this.volumeName = volumeName;
        this.reserveSize = Long.parseLong(sizeWithUnit.replaceAll("[^0-9]", ""));
        this.objectManager = objMgr;
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        Set<Long> hostIds = candidate.getHosts();

        // if one of the host does not have enough free space then return false
        for (Long hostId : hostIds) {
            boolean oneGood = false;

            CacheManager cm = CacheManager.getCacheManagerInstance(this.objectManager);
            HostInfo hostInfo = cm.getHostInfo(hostId);

            // we will get a bunch of disks for that host and we need at least one disk with
            // free space large enough
            oneGood = checkDiskSize(cm, hostInfo, attempt.getInstanceId());
            
            // if no disk with big enough free space for this host, then
            // candidate is no good
            if (!oneGood) {
                return false;
            }
        }
        return true;
    }

    public boolean checkDiskSize (CacheManager cm, HostInfo hostInfo, Long instanceId) {

        for (Entry<String, DiskInfo> entry : hostInfo.getAllDiskInfo()) {
            DiskInfo diskInfo = entry.getValue();
            Long capacity = entry.getValue().getCapacity();
            Long allocatedSize = ((DiskInfo) diskInfo).getAllocatedSize();
            Long freeSize = capacity - allocatedSize;

            InstanceInfo instanceInfo = cm.getInstanceInfoForHost(hostInfo.getHostId(), instanceId);
            
            // we need to consider more than one volume constraints for this
            // container case. That means free size will subtract all reserved
            // size by other volumes as well beside allocated size.
            InstanceDiskReserveInfo diskReserved = instanceInfo.getDiskReserveInfo(((DiskInfo)diskInfo).getDiskDevicePath());
            if (diskReserved != null) {
                freeSize -= diskReserved.getReservedSize();
            }
            if (freeSize >= this.reserveSize) {
                // reserve disk for this instance
                if (diskReserved != null) {
                    diskReserved.addReservedSize(this.reserveSize);
                } else {
                    diskReserved = new InstanceDiskReserveInfo(((DiskInfo)diskInfo).getDiskDevicePath(), this.reserveSize, this.volumeName);
                    instanceInfo.reserveDisk(diskReserved);
                }

                // set a scheduling size on the disk
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("host needs a disk with free space larger than %s GB ", this.reserveSize);
    }
}
