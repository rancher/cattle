package io.github.ibuildthecloud.dstack.eventing;

public interface PoolSpecificListener {

    boolean isAllowQueueing();

    int getQueueDepth();

    String getPoolKey();

}
