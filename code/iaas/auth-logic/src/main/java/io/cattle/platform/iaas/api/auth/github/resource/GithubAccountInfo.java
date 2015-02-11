package io.cattle.platform.iaas.api.auth.github.resource;

public class GithubAccountInfo {
    private final String accountId;
    private final String accountName;

    public GithubAccountInfo(String accountId, String accountName) {
        this.accountId = accountId;
        this.accountName = accountName;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    @Override
    public String toString() {
        return accountName + ":" + accountId;

    }
}
