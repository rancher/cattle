package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class HealthcheckState {

    Long hostId;
    String healthState;
    Long externalTimestamp;

    public HealthcheckState(Long hostId, String healthState) {
        super();
        this.hostId = hostId;
        this.healthState = healthState;
    }

    @Field(typeString="reference[host]")
    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getHealthState() {
        return healthState;
    }

    public void setHealthState(String healthState) {
        this.healthState = healthState;
    }

    public Long getExternalTimestamp() {
        return externalTimestamp;
    }

    public void setExternalTimestamp(Long externalTimestamp) {
        this.externalTimestamp = externalTimestamp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((externalTimestamp == null) ? 0 : externalTimestamp.hashCode());
        result = prime * result + ((healthState == null) ? 0 : healthState.hashCode());
        result = prime * result + ((hostId == null) ? 0 : hostId.hashCode());
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
        HealthcheckState other = (HealthcheckState) obj;
        if (externalTimestamp == null) {
            if (other.externalTimestamp != null)
                return false;
        } else if (!externalTimestamp.equals(other.externalTimestamp))
            return false;
        if (healthState == null) {
            if (other.healthState != null)
                return false;
        } else if (!healthState.equals(other.healthState))
            return false;
        if (hostId == null) {
            if (other.hostId != null)
                return false;
        } else if (!hostId.equals(other.hostId))
            return false;
        return true;
    }

}
