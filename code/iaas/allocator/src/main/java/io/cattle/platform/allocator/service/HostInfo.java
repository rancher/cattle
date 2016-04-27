package io.cattle.platform.allocator.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
    
    public void addDisk(DiskInfo diskInfo)
    {
        this.disks.put(diskInfo.getDiskDevicePath(), diskInfo);
    }
    
    public void removeDisk(String diskDevicePath)
    {
        this.disks.remove(diskDevicePath);
    }

    public DiskInfo getDiskInfo(String diskDevicePath)
    {
        return this.disks.get(diskDevicePath);
    }
    
    public void addInstance(InstanceInfo instanceInfo)
    {
        this.instancesScheduled.put(instanceInfo.getInstanceId(), instanceInfo);
    }
    
    public void removeInstance(Long instanceId)
    {
        this.instancesScheduled.remove(instanceId);
    }
 
    public InstanceInfo getInstanceInfo(Long instanceId)
    {
        return this.instancesScheduled.get(instanceId);
    }

    public  Set<Entry<String, DiskInfo>> getAllDiskInfo() {
        return this.disks.entrySet();
    }

}
