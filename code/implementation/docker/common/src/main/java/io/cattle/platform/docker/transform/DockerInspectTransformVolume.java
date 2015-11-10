package io.cattle.platform.docker.transform;


public class DockerInspectTransformVolume {
    String containerPath;
    String uri;
    String accessMode;
    boolean bindMount;
    String driver;
    String name;
    String externalId;

    public DockerInspectTransformVolume(String cp, String hp, String am, boolean bm, String dr, String name, String externalId) {
        super();
        containerPath = cp;
        uri = hp;
        accessMode = am;
        bindMount = bm;
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
    public String getUri() {
        return uri;
    }
    public void setUri(String hostPath) {
        this.uri = hostPath;
    }
    public String getAccessMode() {
        return accessMode;
    }
    public void setAccessMode(String accessMode) {
        this.accessMode = accessMode;
    }
    public boolean isBindMount() {
        return bindMount;
    }
    public void setBindMount(boolean bindMount) {
        this.bindMount = bindMount;
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
