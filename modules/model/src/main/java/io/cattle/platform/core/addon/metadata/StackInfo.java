package io.cattle.platform.core.addon.metadata;

import io.cattle.platform.core.model.Stack;

public class StackInfo implements MetadataObject {

    long id;
    String name;
    String uuid;
    String environmentUuid;
    String healthState;

    public StackInfo(Stack stack) {
        this.id = stack.getId();
        this.name = stack.getName();
        this.uuid = stack.getUuid();
        this.healthState = stack.getHealthState();
    }

    @Override
    public String getInfoType() {
        return "stack";
    }

    public String getEnvironmentUuid() {
        return environmentUuid;
    }

    public void setEnvironmentUuid(String environmentUuid) {
        this.environmentUuid = environmentUuid;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getHealthState() {
        return healthState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StackInfo stackInfo = (StackInfo) o;

        if (id != stackInfo.id) return false;
        if (name != null ? !name.equals(stackInfo.name) : stackInfo.name != null) return false;
        if (uuid != null ? !uuid.equals(stackInfo.uuid) : stackInfo.uuid != null) return false;
        if (environmentUuid != null ? !environmentUuid.equals(stackInfo.environmentUuid) : stackInfo.environmentUuid != null)
            return false;
        return healthState != null ? healthState.equals(stackInfo.healthState) : stackInfo.healthState == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (environmentUuid != null ? environmentUuid.hashCode() : 0);
        result = 31 * result + (healthState != null ? healthState.hashCode() : 0);
        return result;
    }

}
