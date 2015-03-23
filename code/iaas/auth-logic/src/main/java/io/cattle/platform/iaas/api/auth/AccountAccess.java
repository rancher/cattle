package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.core.model.Account;

import java.util.Collections;
import java.util.List;

public class AccountAccess {

    Account account;
    List<ExternalId> externalIds;

    public AccountAccess() {
    }

    public AccountAccess(Account account, List<ExternalId> externalId) {
        this.account = account;
        this.externalIds = externalId;
        if (this.externalIds == null) {
            this.externalIds = Collections.emptyList();
        }
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public List<ExternalId> getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(List<ExternalId> externalIds) {
        this.externalIds = externalIds;
    }

}