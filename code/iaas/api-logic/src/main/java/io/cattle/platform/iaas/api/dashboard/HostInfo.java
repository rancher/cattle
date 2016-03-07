package io.cattle.platform.iaas.api.dashboard;

import java.util.List;

public class HostInfo {
    private List<Bucket> cores;
    private List<Bucket> memory;
    private List<Bucket> mounts;
    private List<Bucket> networkIn;
    private List<Bucket> networkOut;
    private double networkMaxRxBytesPerSec;
    private double networkMaxTxBytesPerSec;
    private double memoryUsedMB;
    private double memoryTotalMB;
    private double diskUsedMB;
    private double diskTotalMB;
    private double coreCount;

    public HostInfo(List<Bucket> cores, List<Bucket> memory, List<Bucket> mounts, List<Bucket> networkIn,
                    List<Bucket> networkOut, double networkMaxRxBytesPerSec, double networkMaxTxBytesPerSec,
                    double memoryUsedMB, double memoryTotalMB, double diskUsed, double
                            diskTotalMB, double coreCount) {
        this.cores = cores;
        this.memory = memory;
        this.mounts = mounts;
        this.networkIn = networkIn;
        this.networkOut = networkOut;
        this.networkMaxRxBytesPerSec = networkMaxRxBytesPerSec;
        this.networkMaxTxBytesPerSec = networkMaxTxBytesPerSec;
        this.memoryUsedMB = memoryUsedMB;
        this.memoryTotalMB = memoryTotalMB;
        this.diskUsedMB = diskUsed;
        this.diskTotalMB = diskTotalMB;
        this.coreCount = coreCount;
    }

    public List<Bucket> getCores() {
        return cores;
    }

    public void setCores(List<Bucket> cores) {
        this.cores = cores;
    }

    public List<Bucket> getMemory() {
        return memory;
    }

    public void setMemory(List<Bucket> memory) {
        this.memory = memory;
    }

    public List<Bucket> getMounts() {
        return mounts;
    }

    public void setMounts(List<Bucket> mounts) {
        this.mounts = mounts;
    }

    public List<Bucket> getNetworkIn() {
        return networkIn;
    }

    public void setNetworkIn(List<Bucket> networkIn) {
        this.networkIn = networkIn;
    }

    public List<Bucket> getNetworkOut() {
        return networkOut;
    }

    public void setNetworkOut(List<Bucket> networkOut) {
        this.networkOut = networkOut;
    }

    public double getNetworkMaxRxBytesPerSec() {
        return networkMaxRxBytesPerSec;
    }

    public void setNetworkMaxRxBytesPerSec(double networkMaxRxBytesPerSec) {
        this.networkMaxRxBytesPerSec = networkMaxRxBytesPerSec;
    }

    public double getNetworkMaxTxBytesPerSec() {
        return networkMaxTxBytesPerSec;
    }

    public void setNetworkMaxTxBytesPerSec(double networkMaxTxBytesPerSec) {
        this.networkMaxTxBytesPerSec = networkMaxTxBytesPerSec;
    }

    public double getMemoryUsedMB() {
        return memoryUsedMB;
    }

    public void setMemoryUsedMB(double memoryUsedMB) {
        this.memoryUsedMB = memoryUsedMB;
    }

    public double getMemoryTotalMB() {
        return memoryTotalMB;
    }

    public void setMemoryTotalMB(double memoryTotalMB) {
        this.memoryTotalMB = memoryTotalMB;
    }

    public double getDiskUsedMB() {
        return diskUsedMB;
    }

    public void setDiskUsedMB(double diskUsedMB) {
        this.diskUsedMB = diskUsedMB;
    }

    public double getDiskTotalMB() {
        return diskTotalMB;
    }

    public void setDiskTotalMB(double diskTotalMB) {
        this.diskTotalMB = diskTotalMB;
    }

    public double getCoreCount() {
        return coreCount;
    }

    public void setCoreCount(double coreCount) {
        this.coreCount = coreCount;
    }
}
