package io.github.ibuildthecloud.gdapi.version;

import java.util.List;

import javax.inject.Inject;

public class Versions {

    List<String> versions;
    String latest;

    public List<String> getVersions() {
        return versions;
    }

    @Inject
    public void setVersions(List<String> versions) {
        this.versions = versions;
    }

    public String getLatest() {
        return latest;
    }

    @Inject
    public void setLatest(String latest) {
        this.latest = latest;
    }

}
