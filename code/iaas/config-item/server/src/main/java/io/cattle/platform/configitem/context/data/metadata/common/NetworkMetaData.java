package io.cattle.platform.configitem.context.data.metadata.common;

import java.util.Map;

public class NetworkMetaData {
    String name;
    String uuid;
    boolean is_default;
    boolean host_ports;
    protected Map<String, Object> metadata;
    String network_policy;

    public NetworkMetaData(String name, String uuid, boolean hostPorts, Map<String, Object> metadata, boolean isDefault, String networkPolicy) {
        super();
        this.name = name;
        this.uuid = uuid;
        this.metadata = metadata;
        this.host_ports = hostPorts;
        this.is_default = isDefault;
        this.network_policy = networkPolicy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public boolean isHost_ports() {
        return host_ports;
    }

    public void setHost_ports(boolean host_ports) {
        this.host_ports = host_ports;
    }

    public String getNetwork_policy() {
		return network_policy;
	}

	public void setNetwork_policy(String networkPolicy) {
		this.network_policy = networkPolicy;
	}

	public boolean isIs_default() {
        return is_default;
    }

    public void setIs_default(boolean is_default) {
        this.is_default = is_default;
    }

}
