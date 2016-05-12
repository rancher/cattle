package io.cattle.platform.allocator.service;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;

import io.cattle.platform.allocator.util.AllocatorUtils;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class HostInfo {

    private Long hostId;

    // a bunch of disks keyed on disk device path
    private Map<String, DiskInfo> disks;

    // a bunch of scheduling instance, keyed on instance ID
    private Map<Long, InstanceInfo> instancesScheduled;

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public HostInfo(Long hostId) {
        this.setHostId(hostId);
        this.disks = new HashMap<String, DiskInfo>();
        this.instancesScheduled = new HashMap<Long, InstanceInfo>();
    }

    public synchronized void addDisk(DiskInfo diskInfo) {
        this.disks.put(diskInfo.getDiskDevicePath(), diskInfo);
    }

    public DiskInfo getDiskInfo(String diskDevicePath) {
        return this.disks.get(diskDevicePath);
    }

    public synchronized void addInstance(InstanceInfo instanceInfo) {
        this.instancesScheduled.put(instanceInfo.getInstanceId(), instanceInfo);
    }

    public synchronized InstanceInfo removeInstance(Long instanceId) {
        return this.instancesScheduled.remove(instanceId);
    }

    public InstanceInfo getInstanceInfo(Long instanceId) {
        return this.instancesScheduled.get(instanceId);
    }

    public Set<Entry<String, DiskInfo>> getAllDiskInfo() {
        return this.disks.entrySet();
    }

    public void loadAllocatedInstanceResource(ObjectManager objectManager) {
        // load all instances that consume host resources into hostInfo. Then
        // account for those resources for hostInfo.
        List<InstanceHostMap> instanceHostMappings = objectManager.find(InstanceHostMap.class,
                INSTANCE_HOST_MAP.HOST_ID, this.hostId, INSTANCE_HOST_MAP.REMOVED, null);

        for (InstanceHostMap mapping : instanceHostMappings) {
            Long instanceId = mapping.getInstanceId();
            Instance instance = objectManager.findAny(Instance.class, INSTANCE.ID, instanceId, INSTANCE.REMOVED, null);
            if (instance == null) {
                continue;
            }
            Map<Pair<String, Long>, DiskInfo> volumeToDiskMapping = AllocatorUtils.allocateDiskForVolumes(this.hostId, instance, objectManager);
            if (volumeToDiskMapping == null) {
                continue;
            }

            for (Entry<Pair<String, Long>, DiskInfo> volumeToDisk : volumeToDiskMapping.entrySet()) {
                Pair<String, Long> vol = volumeToDisk.getKey();
                DiskInfo disk = volumeToDisk.getValue();

                // account for disk size resource
                disk.addAllocatedSize(vol.getRight());

                InstanceInfo instanceInfo = this.getInstanceInfo(instanceId);
                if (instanceInfo == null) {
                    continue;
                }
                // record to cache for deletion purpose
                instanceInfo.addReservedSize(disk.getDiskDevicePath(), vol.getRight());
            }
        }
    }

}
