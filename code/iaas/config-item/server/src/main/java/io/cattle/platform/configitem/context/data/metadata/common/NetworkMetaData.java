package io.cattle.platform.configitem.context.data.metadata.common;

import java.util.Map;

public class NetworkMetaData {
    String name;
    String uuid;
    boolean is_default;
    boolean host_ports;
    protected Map<String, Object> metadata;
    String default_policy_action;
    Object policy;
    
 // helper field needed by metadata service to process object
    String metadata_kind;

    public NetworkMetaData(String name, String uuid, boolean is_default, boolean host_ports,
            Map<String, Object> metadata, String defaultPolicyAction, Object policy) {
        super();
        this.name = name;
        this.uuid = uuid;
        this.is_default = is_default;
        this.host_ports = host_ports;
        this.metadata = metadata;
        this.default_policy_action = defaultPolicyAction;
        this.policy = policy;
        this.metadata_kind = "network";
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

    public Boolean isHost_ports() {
        return host_ports;
    }

    public void setHost_ports(Boolean host_ports) {
        this.host_ports = host_ports;
    }

    public boolean isIs_default() {
        return is_default;
    }

    public void setIs_default(boolean is_default) {
        this.is_default = is_default;
    }

    public String getDefault_policy_action() {
        return default_policy_action;
    }

    public void setDefault_policy_action(String defaultPolicyAction) {
        this.default_policy_action = defaultPolicyAction;
    }

    public Object getPolicy() {
        return policy;
    }

    public void setPolicy(Object policy) {
        this.policy = policy;
    }

    public String getMetadata_kind() {
        return metadata_kind;
    }

    public void setMetadata_kind(String metadata_kind) {
        this.metadata_kind = metadata_kind;
    }
}
