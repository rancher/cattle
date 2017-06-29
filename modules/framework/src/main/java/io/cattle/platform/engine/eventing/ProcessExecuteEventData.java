package io.cattle.platform.engine.eventing;

public class ProcessExecuteEventData {

    String resourceId;
    String resourceType;
    Long accountId;
    String name;

    public ProcessExecuteEventData() {
    }

    public ProcessExecuteEventData(String name, String resourceType, String resourceId, Long accountId) {
        super();
        this.name = name;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.accountId = accountId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
