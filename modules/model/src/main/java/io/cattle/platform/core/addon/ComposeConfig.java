package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ComposeConfig {
    String dockerComposeConfig;
    String rancherComposeConfig;

    public ComposeConfig(String dockerComposeConfig, String rancherComposeConfig) {
        super();
        this.dockerComposeConfig = dockerComposeConfig;
        this.rancherComposeConfig = rancherComposeConfig;
    }

    public ComposeConfig() {
    }

    public String getDockerComposeConfig() {
        return dockerComposeConfig;
    }

    public void setDockerComposeConfig(String dockerComposeConfig) {
        this.dockerComposeConfig = dockerComposeConfig;
    }

    public String getRancherComposeConfig() {
        return rancherComposeConfig;
    }

    public void setRancherComposeConfig(String rancherComposeConfig) {
        this.rancherComposeConfig = rancherComposeConfig;
    }
}
