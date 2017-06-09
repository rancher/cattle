package io.cattle.platform.hazelcast.membership;

import io.cattle.platform.engine.server.Cluster;

import org.apache.commons.lang3.tuple.Pair;

public interface ClusterService extends Cluster {

    boolean isMaster();

    boolean waitReady();

    ClusteredMember getMaster();

    @Override
    Pair<Integer, Integer> getCountAndIndex();

}