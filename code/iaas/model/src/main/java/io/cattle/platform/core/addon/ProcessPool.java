package io.cattle.platform.core.addon;

public class ProcessPool {

    String name;
    int activeTasks, poolSize, maxPoolSize, minPoolSize, queueSize, queueRemainingCapacity;
    long completedTasks, rejectedTasks;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getActiveTasks() {
        return activeTasks;
    }

    public void setActiveTasks(int activeTasks) {
        this.activeTasks = activeTasks;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(long completedTasks) {
        this.completedTasks = completedTasks;
    }

    public long getRejectedTasks() {
        return rejectedTasks;
    }

    public void setRejectedTasks(long rejectedTasks) {
        this.rejectedTasks = rejectedTasks;
    }

    public int getQueueRemainingCapacity() {
        return queueRemainingCapacity;
    }

    public void setQueueRemainingCapacity(int queueRemainingCapacity) {
        this.queueRemainingCapacity = queueRemainingCapacity;
    }

}