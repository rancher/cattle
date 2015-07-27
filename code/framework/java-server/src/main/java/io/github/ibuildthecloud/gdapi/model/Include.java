package io.github.ibuildthecloud.gdapi.model;

import java.util.List;

public class Include {

    List<String> links;

    public Include(List<String> links) {
        this.links = links;
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }
}
