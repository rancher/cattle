package io.cattle.platform.metadata.model;

import io.cattle.platform.core.model.Stack;

public class StackInfo implements MetadataObject {

    long id;
    String name;
    String uuid;
    String healthState;
    boolean system;

    public StackInfo(Stack stack) {
        this.id = stack.getId();
        this.name = stack.getName();
        this.uuid = stack.getUuid();
        this.system = stack.getSystem();
        this.healthState = stack.getHealthState();
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

    public boolean isSystem() {
        return system;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((healthState == null) ? 0 : healthState.hashCode());
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (system ? 1231 : 1237);
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StackInfo other = (StackInfo) obj;
        if (healthState == null) {
            if (other.healthState != null)
                return false;
        } else if (!healthState.equals(other.healthState))
            return false;
        if (id != other.id)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (system != other.system)
            return false;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

    public String getHealthState() {
        return healthState;
    }

}
