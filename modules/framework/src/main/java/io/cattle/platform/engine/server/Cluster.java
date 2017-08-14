package io.cattle.platform.engine.server;

public interface Cluster {

    boolean isInPartition(Long accountId, Long clusterId);

}
