package io.cattle.platform.allocator.service.impl;

public class ComputeResourceRequest implements ResourceRequest {

    public ComputeResourceRequest(String resource, Long amount, String type) {
        super();
        this.resource = resource;
        this.amount = amount;
        this.type = type;
    }

    private String resource;
    
    private Long amount;
    
    private String type;
    
    @Override
    public String getResource() {
        return this.resource;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public void setResource(String resourceType) {
        this.resource = resourceType;
    }
    
    public String toString() {
        return String.format("ResourceType: %s, Amount: %s", resource, amount);
    }
    
    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
