package io.cattle.platform.iaas.api.auth.github.resource;

public class TeamAccountInfo {

    private final String org;
    private final String name;
    private final String id;
    
    public TeamAccountInfo(String org, String name, String id) {
        this.org = org;
        this.name = name;
        this.id = id;
    }
    
    public String getOrg() {
        return org;
    }
    
    public String getName() {
        return name;
    }
    
    public String getId() {
        return id;
    }
}
