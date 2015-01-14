package io.cattle.platform.iaas.api.auth.github;

import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = "githubconfig")
public class GithubConfig {

    private Boolean enabled;
    private String clientId;
    private List<String> allowedUsers;
    private List<String> allowedOrganizations;

    public GithubConfig(Boolean enabled, String clientId, List<String> allowedUsers, List<String> allowedOrganizations) {
        this.enabled = enabled;
        this.clientId = clientId;
        this.allowedUsers = allowedUsers;
        this.allowedOrganizations = allowedOrganizations;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public List<String> getAllowedUsers() {
        return allowedUsers;
    }

    public List<String> getAllowedOrganizations() {
        return allowedOrganizations;
    }

}
