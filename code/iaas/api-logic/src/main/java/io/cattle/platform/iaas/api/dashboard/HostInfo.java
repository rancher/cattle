package io.cattle.platform.iaas.api.dashboard;

import java.util.List;

public class HostInfo {
    private List<Bucket> cores;
    private List<Bucket> memory;
    private List<Bucket> mounts;
    private List<Bucket> networkIn;
    private List<Bucket> networkOut;
    private double networkMaxRx;
    private double networkMaxTx;
    private double networkTotalUsedRx;
    private double networkTotalUsedTx;
    private double memoryUsed;
    private double memoryTotal;
    private double diskUsed;
    private double diskTotal;
    private double coreCount;

    public HostInfo(List<Bucket> cores, List<Bucket> memory, List<Bucket> mounts, List<Bucket> networkIn,
                    List<Bucket> networkOut, double networkMaxRx, double networkMaxTx, double networkTotalUsedRx,
                    double networkTotalUsedTx, double memoryUsed, double memoryTotal, double diskUsed, double
                            diskTotal, double coreCount) {
        this.cores = cores;
        this.memory = memory;
        this.mounts = mounts;
        this.networkIn = networkIn;
        this.networkOut = networkOut;
        this.networkMaxRx = networkMaxRx;
        this.networkMaxTx = networkMaxTx;
        this.networkTotalUsedRx = networkTotalUsedRx;
        this.networkTotalUsedTx = networkTotalUsedTx;
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

    public double getNetworkMaxRx() {
        return networkMaxRx;
    }

    public void setNetworkMaxRx(double networkMaxRx) {
        this.networkMaxRx = networkMaxRx;
    }

    public double getNetworkMaxTx() {
        return networkMaxTx;
    }

    public void setNetworkMaxTx(double networkMaxTx) {
        this.networkMaxTx = networkMaxTx;
    }

    public double getNetworkTotalUsedRx() {
        return networkTotalUsedRx;
    }

    public void setNetworkTotalUsedRx(double networkTotalUsedRx) {
        this.networkTotalUsedRx = networkTotalUsedRx;
    }

    public double getNetworkTotalUsedTx() {
        return networkTotalUsedTx;
    }

    public void setNetworkTotalUsedTx(double networkTotalUsedTx) {
        this.networkTotalUsedTx = networkTotalUsedTx;
    }

    public double getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(double memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public double getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(double memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public double getDiskUsed() {
        return diskUsed;
    }

    public void setDiskUsed(double diskUsed) {
        this.diskUsed = diskUsed;
    }

    public double getDiskTotal() {
        return diskTotal;
    }

    public void setDiskTotal(double diskTotal) {
        this.diskTotal = diskTotal;
    }

    public double getCoreCount() {
        return coreCount;
    }

    public void setCoreCount(double coreCount) {
        this.coreCount = coreCount;
    }
}
