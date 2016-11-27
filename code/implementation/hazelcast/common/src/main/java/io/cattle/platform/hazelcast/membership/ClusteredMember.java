package io.cattle.platform.hazelcast.membership;

public class ClusteredMember {

    Long id;
    ClusterConfig config;
    boolean self, clustered;

    public ClusteredMember(Long id, ClusterConfig config, boolean self, boolean clustered) {
        super();
        this.id = id;
        this.config = config;
        this.self = self;
        this.clustered = clustered;
    }

    public String getAdvertiseAddress() {
        String address = config.getAdvertiseAddress();
        return address.split(":")[0];
    }

    public Integer getHttpPort() {
        return config.getHttpPort();
    }

    public Long getId() {
        return id;
    }

    public boolean isSelf() {
        return self;
    }

    public boolean isClustered() {
        return clustered;
    }

    public ClusterConfig getConfig() {
        return config;
    }

}