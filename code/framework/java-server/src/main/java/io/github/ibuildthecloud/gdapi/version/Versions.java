package io.github.ibuildthecloud.gdapi.version;

import java.util.Set;

import javax.inject.Inject;

public class Versions {

    Set<String> versions;
    String latest;
    String rootVersion;

    public Set<String> getVersions() {
        return versions;
    }

    @Inject
    public void setVersions(Set<String> versions) {
        this.versions = versions;
    }

    public String getLatest() {
        return latest;
    }

    @Inject
    public void setLatest(String latest) {
        this.latest = latest;
    }

    public String getRootVersion() {
        return rootVersion;
    }

    public void setRootVersion(String rootVersion) {
        this.rootVersion = rootVersion;
    }

}
