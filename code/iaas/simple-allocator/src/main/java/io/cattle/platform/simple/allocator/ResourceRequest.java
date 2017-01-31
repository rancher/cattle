package io.cattle.platform.simple.allocator;

public class ResourceRequest {

    private String resource;
    private Long amount;

    public String getResource() {
        return resource;
    }
    public void setResource(String resource) {
        this.resource = resource;
    }
    public Long getAmount() {
        return amount;
    }
    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String toString() {
        return String.format("Resource: %s, amount: %s", resource, amount);
    }
}
