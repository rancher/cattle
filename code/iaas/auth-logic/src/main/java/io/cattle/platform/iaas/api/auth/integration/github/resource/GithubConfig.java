package io.cattle.platform.iaas.api.auth.integration.github.resource;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(name = GithubConstants.CONFIG)
public class GithubConfig {

    private String hostname;
    private String scheme;
    private Boolean enabled;
    private String accessMode;
    private String clientId;
    private String clientSecret;
    private List<Identity> allowedIdentities;

    public GithubConfig(Boolean enabled, String accessMode, String clientId, String hostName,
                        String scheme, List<Identity> allowedIdentities) {
        this.enabled = enabled;
        this.accessMode = accessMode;
        this.clientId = clientId;
        this.hostname = hostName;
        this.scheme = scheme;
        this.allowedIdentities = allowedIdentities;
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

    @Field(nullable = false)
    public String getAccessMode() {
        return accessMode;
    }

    @Field(nullable = true)
    public String getHostname() {
        return hostname;
    }

    @Field(nullable = true)
    public String getScheme() {
        return scheme;
    }

    public String getName() {
        return GithubConstants.CONFIG;
    }

    @Field(nullable = true)
    public List<Identity> getAllowedIdentities() {
        return allowedIdentities;
    }
}
