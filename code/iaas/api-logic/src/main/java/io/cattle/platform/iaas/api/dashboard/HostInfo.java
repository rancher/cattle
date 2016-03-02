package io.cattle.platform.iaas.api.dashboard;

import java.util.List;

public class HostInfo {
    private List<Bucket> cores;
    private List<Bucket> memory;
    private List<Bucket> mounts;
    private List<Bucket> networkIn;
    private List<Bucket> networkOut;
    private long networkMax;
    private long memoryUsed;
    private long memoryTotal;
    private long diskUsed;
    private long diskTotal;
    private long coreCount;

    public HostInfo(List<Bucket> cores, List<Bucket> memory, List<Bucket> mounts, List<Bucket> networkIn,
                    List<Bucket> networkOut, long networkMax, long memoryUsed, long memoryTotal, long diskUsed, long
                            diskTotal, long coreCount) {
        this.cores = cores;
        this.memory = memory;
        this.mounts = mounts;
        this.networkIn = networkIn;
        this.networkOut = networkOut;
        this.networkMax = networkMax;
        this.memoryUsed = memoryUsed;
        this.memoryTotal = memoryTotal;
        this.diskUsed = diskUsed;
        this.diskTotal = diskTotal;
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

    public long getNetworkMax() {
        return networkMax;
    }

    public void setNetworkMax(long networkMax) {
        this.networkMax = networkMax;
    }

    public long getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(long memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public long getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(long memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public long getDiskUsed() {
        return diskUsed;
    }

    public void setDiskUsed(long diskUsed) {
        this.diskUsed = diskUsed;
    }

    public long getDiskTotal() {
        return diskTotal;
    }

    public void setDiskTotal(long diskTotal) {
        this.diskTotal = diskTotal;
    }

    public long getCoreCount() {
        return coreCount;
    }

    public void setCoreCount(long coreCount) {
        this.coreCount = coreCount;
    }
}
