package io.cattle.platform.docker.transform;


public class DockerInspectTransformVolume {
    String containerPath;
    String hostPath;
    String accessMode;
    boolean bindMount;
    
    public DockerInspectTransformVolume(String cp, String hp, String am, boolean bm) {
        super();
        containerPath = cp;
        hostPath = hp;
        accessMode = am;
        bindMount = bm;
    }
    
    public String getContainerPath() {
        return containerPath;
    }
    public void setContainerPath(String containerPath) {
        this.containerPath = containerPath;
    }
    public String getHostPath() {
        return hostPath;
    }
    public void setHostPath(String hostPath) {
        this.hostPath = hostPath;
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
}
