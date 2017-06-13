package io.cattle.platform.hazelcast.membership;

import io.cattle.platform.engine.server.Cluster;

public interface ClusterService extends Cluster {

    boolean isMaster();

    boolean waitReady();

    ClusteredMember getMaster();

}