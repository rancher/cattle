package io.github.ibuildthecloud.dstack.configitem.request;

public class ConfigUpdateItem {

    String name;
    Long requestedVersion;
    boolean apply = true;
    boolean increment = true;
    boolean checkInSync = false;

    public ConfigUpdateItem() {
    }

    public ConfigUpdateItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConfigUpdateItem withName(String name) {
        this.name = name;
        return this;
    }

    public Long getRequestedVersion() {
        return requestedVersion;
    }

    public void setRequestedVersion(Long requestedVersion) {
        this.requestedVersion = requestedVersion;
    }

    public ConfigUpdateItem withRequestedVersion(Long requestedVersion) {
        this.requestedVersion = requestedVersion;
        return this;
    }

    public boolean isApply() {
        return apply;
    }

    public void setApply(boolean apply) {
        this.apply = apply;
    }

    public ConfigUpdateItem withApply(boolean apply) {
        this.apply = apply;
        return this;
    }

    public boolean isIncrement() {
        return increment;
    }

    public void setIncrement(boolean increment) {
        this.increment = increment;
    }

    public ConfigUpdateItem withIncrement(boolean increment) {
        this.increment = increment;
        return this;
    }

    public boolean isCheckInSync() {
        return checkInSync;
    }

    public void setCheckInSync(boolean checkInSync) {
        this.checkInSync = checkInSync;
    }

    public ConfigUpdateItem withCheckInSync(boolean checkInSync) {
        this.checkInSync = checkInSync;
        return this;
    }

}
