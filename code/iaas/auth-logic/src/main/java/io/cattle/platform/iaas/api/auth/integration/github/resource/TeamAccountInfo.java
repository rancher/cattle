package io.cattle.platform.iaas.api.auth.integration.github.resource;

public class TeamAccountInfo {

    private final String org;
    private final String name;
    private final String id;
    private final String slug;

    public TeamAccountInfo(String org, String name, String id, String slug) {
        this.org = org;
        this.name = name;
        this.id = id;
        this.slug = slug;
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

    public String getSlug() {
        return slug;
    }
}
