package io.cattle.platform.metadata.model;

import io.cattle.platform.core.model.Account;

public class EnvironmentInfo implements MetadataObject  {

    String uuid;
    String externalId;
    long accountId;

    public EnvironmentInfo() {
    }

    public EnvironmentInfo(Account account) {
        accountId = account.getId();
        uuid = account.getUuid();
        externalId = account.getExternalId();
    }

    public long getAccountId() {
        return accountId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getExternalId() {
        return externalId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnvironmentInfo that = (EnvironmentInfo) o;

        if (accountId != that.accountId) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;
        return externalId != null ? externalId.equals(that.externalId) : that.externalId == null;
    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (externalId != null ? externalId.hashCode() : 0);
        result = 31 * result + (int) (accountId ^ (accountId >>> 32));
        return result;
    }

}
