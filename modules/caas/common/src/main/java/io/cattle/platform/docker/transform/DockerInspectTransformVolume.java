package io.cattle.platform.docker.transform;

public class DockerInspectTransformVolume {
    String containerPath;
    String accessMode;
    String driver;
    String name;
    String externalId;

    public DockerInspectTransformVolume(String cp, String am, String dr, String name, String externalId) {
        super();
        containerPath = cp;
        accessMode = am;
        driver = dr;
        this.name = name;
        this.externalId = externalId;
    }

    public String getContainerPath() {
        return containerPath;
    }

    public void setContainerPath(String containerPath) {
        this.containerPath = containerPath;
    }

    public String getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(String accessMode) {
        this.accessMode = accessMode;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

}