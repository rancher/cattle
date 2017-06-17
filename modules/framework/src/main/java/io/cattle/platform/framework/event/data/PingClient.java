package io.cattle.platform.framework.event.data;

public class PingClient {
    Long agentId;
    Long clusterId;

    public PingClient() {
    }

    public PingClient(Long agentId, Long clusterId) {
        this.agentId = agentId;
        this.clusterId = clusterId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }
}
