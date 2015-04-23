package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.core.model.Account;

import java.util.HashSet;
import java.util.Set;

public class AccountAccess {

    Account account;
    Set<ExternalId> externalIds;

    public AccountAccess() {
    }

    public AccountAccess(Account account, Set<ExternalId> externalIdSet) {
        this.account = account;
        this.externalIds = externalIdSet;
        if (this.externalIds == null) {
            this.externalIds = new HashSet<>();
        }
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Set<ExternalId> getExternalIds() {
        return externalIds;
    }

}
