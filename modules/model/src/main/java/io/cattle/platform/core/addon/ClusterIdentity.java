package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;
import java.util.Map;

@Type(list = false)
public class ClusterIdentity {

    String username;
    String userId;
    List<String> groups;
    Map<String, String> attributes;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
