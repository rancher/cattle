package io.cattle.platform.allocator.service;

public class InstanceDiskReserveInfo {

    private String diskDevicePath;
    private Long reservedSize;
    private String volumeName;
    private boolean allocated;

    public boolean isAllocated() {
        return allocated;
    }

    public void setAllocated(boolean allocated) {
        this.allocated = allocated;
    }

    public InstanceDiskReserveInfo(String diskDevicePath, Long reservedSize, String volumeName) {
        this.diskDevicePath = diskDevicePath;
        this.reservedSize = reservedSize;
        this.volumeName = volumeName;
    }
    
    public String getDiskDevicePath() {
        return diskDevicePath;
    }

    public void setDiskDevicePath(String diskDevicePath) {
        this.diskDevicePath = diskDevicePath;
    }

    public Long getReservedSize() {
        return reservedSize;
    }

    public void addReservedSize(Long reserveSize) {
        this.reservedSize += reserveSize;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

}