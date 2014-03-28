package io.cattle.platform.eventing;

public interface PoolSpecificListener {

    boolean isAllowQueueing();

    int getQueueDepth();

    String getPoolKey();

}
