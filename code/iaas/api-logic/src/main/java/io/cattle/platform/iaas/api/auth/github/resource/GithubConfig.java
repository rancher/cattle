package io.cattle.platform.iaas.api.auth.github.resource;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = "githubconfig")
public class GithubConfig {

    private Boolean enabled;
    private String clientId;
    private String clientSecret;
    private List<String> allowedUsers;
    private List<String> allowedOrganizations;

    public GithubConfig(Boolean enabled, String clientId, List<String> allowedUsers, List<String> allowedOrganizations) {
        this.enabled = enabled;
        this.clientId = clientId;
        this.allowedUsers = allowedUsers;
        this.allowedOrganizations = allowedOrganizations;
    }

    @Field(nullable = true)
    public Boolean getEnabled() {
        return enabled;
    }

    @Field(nullable = true)
    public String getClientId() {
        return clientId;
    }
    
    @Field(nullable = true)
    public String getClientSecret() {
        return clientSecret;
    }

    @Field(nullable = true)
    public List<String> getAllowedUsers() {
        return allowedUsers;
    }

    @Field(nullable = true)
    public List<String> getAllowedOrganizations() {
        return allowedOrganizations;
    }

}
