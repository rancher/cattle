package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ComposeConfig {
    String dockerComposeConfig;

    public ComposeConfig(String dockerComposeConfig) {
        super();
        this.dockerComposeConfig = dockerComposeConfig;
    }

    public ComposeConfig() {
    }

    public String getDockerComposeConfig() {
        return dockerComposeConfig;
    }

    public void setDockerComposeConfig(String dockerComposeConfig) {
        this.dockerComposeConfig = dockerComposeConfig;
    }
}
