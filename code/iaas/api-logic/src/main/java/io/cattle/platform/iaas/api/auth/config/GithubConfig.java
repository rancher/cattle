package io.cattle.platform.iaas.api.auth.config;

import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = "githubconfig")
public class GithubConfig {

    private Boolean enabled;
    private String clientId;
    private List<String> allowUsers;
    private List<String> allowOrganizations;

    public GithubConfig(Boolean enabled, String clientId, List<String> allowUsers, List<String> allowOrganizations) {
        this.enabled = enabled;
        this.clientId = clientId;
        this.allowUsers = allowUsers;
        this.allowOrganizations = allowOrganizations;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public List<String> getAllowUsers() {
        return allowUsers;
    }

    public List<String> getAllowOrganizations() {
        return allowOrganizations;
    }

}
