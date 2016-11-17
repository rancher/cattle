package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class HealthcheckState {

    public String hostId;
    public String healthState;

    public HealthcheckState(String hostId, String healthState) {
        super();
        this.hostId = hostId;
        this.healthState = healthState;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHealthState() {
        return healthState;
    }

    public void setHealthState(String healthState) {
        this.healthState = healthState;
    }

}
