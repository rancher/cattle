package io.cattle.platform.engine.model;

public class ProcessReference {

    Long id, resourceId, accountId, clusterId;
    String resourceType, name;

    public ProcessReference(Long id, String name, String resourceType, String resourceId, Long accountId, Long clusterId) {
        super();
        this.id = id;
        this.name = name;
        this.accountId = accountId;
        this.clusterId = clusterId;
        this.resourceType = resourceType;
        this.resourceId = new Long(resourceId);
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Long getId() {
        return id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceKey() {
        return String.format("%s:%s", resourceType, resourceId);
    }

    public String getName() {
        return name;
    }

}