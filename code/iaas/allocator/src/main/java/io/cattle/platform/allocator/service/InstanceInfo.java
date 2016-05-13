package io.cattle.platform.allocator.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class InstanceInfo {

    private Long instanceId;
    private Map<String, Long> reserveDisks = new HashMap<String, Long>();

    public InstanceInfo(Long instanceId, Long hostId) {
        super();
        this.instanceId = instanceId;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public synchronized void addReservedSize(String diskDevicePath, Long reserveSize) {
        Long currentSize = this.reserveDisks.get(diskDevicePath);
        if (currentSize == null) {
            currentSize = 0L;
        }
        this.reserveDisks.put(diskDevicePath, currentSize + reserveSize);
    }

    public synchronized void releaseDisk(String diskDevicePath) {
        this.reserveDisks.remove(diskDevicePath);
    }


    public  Set<Entry<String, Long>> getAllocatedDisks() {
        return reserveDisks.entrySet();
    }


}