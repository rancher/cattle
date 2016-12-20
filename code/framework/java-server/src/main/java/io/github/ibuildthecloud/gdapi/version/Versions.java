package io.github.ibuildthecloud.gdapi.version;

import java.util.Set;

public class Versions {

    Set<String> versions;
    String latest;
    String rootVersion;

    public Set<String> getVersions() {
        return versions;
    }

    public void setVersions(Set<String> versions) {
        this.versions = versions;
    }

    public String getLatest() {
        return latest;
    }

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
