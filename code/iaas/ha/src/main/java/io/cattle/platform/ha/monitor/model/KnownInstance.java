package io.cattle.platform.ha.monitor.model;

import java.util.Date;
import java.util.Map;

public class KnownInstance {
    String uuid;
    String state;
    String externalId;
    Date removed;
    Map<String, Object>data;

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
    public Date getRemoved() {
        return removed;
    }
    public void setRemoved(Date removed) {
        this.removed = removed;
    }
    public Map<String, Object> getData() {
        return data;
    }
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
