package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ComposeConfig {
    String dockerComposeConfig;
    String rancherComposeConfig;
    String composeConfig;
    
    public ComposeConfig(String dockerComposeConfig, String rancherComposeConfig, String composeConfig) {
        super();
        this.dockerComposeConfig = dockerComposeConfig;
        this.rancherComposeConfig = rancherComposeConfig;
        this.composeConfig = composeConfig;
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
    
    public String getComposeConfig() {
        return composeConfig;
    }

    public void setComposeConfig(String composeConfig) {
        this.composeConfig = composeConfig;
    }
}
