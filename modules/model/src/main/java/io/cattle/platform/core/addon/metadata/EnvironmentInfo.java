package io.cattle.platform.core.addon.metadata;

import io.cattle.platform.core.model.Account;
import io.github.ibuildthecloud.gdapi.annotation.Field;

public class EnvironmentInfo implements MetadataObject {

    long id;
    String uuid;
    String environmentUuid;
    String externalId;
    String name;
    boolean system;
    long accountId;

    public EnvironmentInfo() {
    }

    @Field(typeString = "reference[account]")
    public Long getInfoTypeId() {
        return id;
    }

    public EnvironmentInfo(Account account) {
        id = account.getId();
        accountId = account.getId();
        uuid = account.getUuid();
        externalId = account.getExternalId();
        system = account.getClusterOwner();
        name = account.getName();
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    @Override
    public String getInfoType() {
        return "environment";
    }

    public boolean isSystem() {
        return system;
    }

    @Field(typeString = "reference[account]")
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
    public String getEnvironmentUuid() {
        return environmentUuid;
    }

    @Override
    public void setEnvironmentUuid(String environmentUuid) {
        this.environmentUuid = environmentUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnvironmentInfo that = (EnvironmentInfo) o;

        if (id != that.id) return false;
        if (system != that.system) return false;
        if (accountId != that.accountId) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;
        if (environmentUuid != null ? !environmentUuid.equals(that.environmentUuid) : that.environmentUuid != null)
            return false;
        if (externalId != null ? !externalId.equals(that.externalId) : that.externalId != null) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (environmentUuid != null ? environmentUuid.hashCode() : 0);
        result = 31 * result + (externalId != null ? externalId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (system ? 1 : 0);
        result = 31 * result + (int) (accountId ^ (accountId >>> 32));
        return result;
    }
}
