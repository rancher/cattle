package io.cattle.platform.iaas.api.auth.integration.github.resource;

import io.cattle.platform.api.auth.Identity;

public class GithubAccountInfo {
    private final String accountId;
    private final String accountName;
    private final String profilePicture;
    private final String profileUrl;
    private final String name;

    public GithubAccountInfo(String accountId, String accountName, String profilePicture, String profileUrl, String name) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.profilePicture = profilePicture;
        this.profileUrl = profileUrl;
        this.name = name;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    @Override
    public String toString() {
        return accountName + ':' + accountId + ':' + profilePicture + ':' + profileUrl;

    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public Identity toIdentity(String scope) {
        return new Identity(scope, accountId, name, profileUrl, profilePicture, accountName);
    }

    public String getName() {
        return name;
    }
}
