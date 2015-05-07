package io.cattle.platform.ha.monitor.model;

import java.util.Date;

public class KnownInstance {
    String uuid;
    String state;
    String externalId;
    String systemContainer;
    String instanceTriggeredStop;
    Date removed;
    
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public String getExternalId() {
        return externalId;
    }
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
    public String getSystemContainer() {
        return systemContainer;
    }
    public void setSystemContainer(String systemContainer) {
        this.systemContainer = systemContainer;
    }
    public String getInstanceTriggeredStop() {
        return instanceTriggeredStop;
    }
    public void setInstanceTriggeredStop(String instanceTriggeredStop) {
        this.instanceTriggeredStop = instanceTriggeredStop;
    }
    public Date getRemoved() {
        return removed;
    }
    public void setRemoved(Date removed) {
        this.removed = removed;
    }
    
}
