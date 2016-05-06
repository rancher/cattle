package io.cattle.platform.allocator.service;

public class DiskInfo {
    private String diskDevicePath;
    private Long capacity;
    private Long allocatedSize;

    public DiskInfo(String diskDevicePath, Long capacity, Long used) {
        this.diskDevicePath = diskDevicePath;
        this.capacity = capacity;
        this.allocatedSize = used;
    }

    public String getDiskDevicePath() {
        return diskDevicePath;
    }

    public Long getCapacity() {
        return capacity;
    }

    public Long getAllocatedSize() {
        return allocatedSize;
    }

    public void addAllocatedSize(Long allocateSize) {
        this.allocatedSize += allocateSize;
    }

    public void freeAllocatedSize(Long freeSize) {
        this.allocatedSize -= freeSize;
    }
}
