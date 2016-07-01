package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class TargetPortRule {

    String hostname;
    String path;
    Integer targetPort;
    String backendName;

    public TargetPortRule(String hostname, String path, Integer targetPort, String backendName) {
        super();
        this.hostname = hostname;
        this.path = path;
        this.targetPort = targetPort;
        this.backendName = backendName;
    }

    public TargetPortRule() {
    }

    @Field(nullable = true)
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Field(nullable = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Field(min = 1, max = 65535, required = true)
    public Integer getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(Integer targetPort) {
        this.targetPort = targetPort;
    }

    @Field(nullable = true)
    public String getBackendName() {
        return backendName;
    }

    public void setBackendName(String backendName) {
        this.backendName = backendName;
    }

}
