package io.cattle.platform.configitem.context.data.metadata.common;

import io.cattle.platform.core.model.Account;

public class StackMetaData {
    String environment_name;
    String environment_uuid;
    String name;
    String uuid;
    Boolean system;

    // helper field needed by metadata service to process object
    String metadata_kind;

    public StackMetaData(String stackName, String stackUUID, boolean isSystem, Account account) {
        this.name = stackName;
        this.uuid = stackUUID;
        this.environment_name = account.getName();
        this.environment_uuid = account.getUuid();
        this.system = isSystem;
        this.metadata_kind = "stack";
    }

    public String getEnvironment_uuid() {
        return environment_uuid;
    }

    public void setEnvironment_uuid (String environment_uuid) {
        this.environment_uuid = environment_uuid;
    }

    public String getEnvironment_name() {
        return environment_name;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setEnvironment_name(String environment_name) {
        this.environment_name = environment_name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Boolean getSystem() {
        return system;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }

    public String getMetadata_kind() {
        return metadata_kind;
    }

    public void setMetadata_kind(String metadata_kind) {
        this.metadata_kind = metadata_kind;
    }


}
