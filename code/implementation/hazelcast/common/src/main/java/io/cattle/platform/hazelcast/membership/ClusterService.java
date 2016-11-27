package io.cattle.platform.hazelcast.membership;

public interface ClusterService {

    boolean isMaster();

    boolean waitReady();

    ClusteredMember getMaster();

}