package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.Map;

@Type(list = false)
public class LogConfig {

    String driver;
    Map<String, String> config;

    public Map<String, String> getConfig() {
        return config;
    }

    @Field(nullable = true)
    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public String getDriver() {
        return driver;
    }

    @Field(nullable = true)
    public void setDriver(String driver) {
        this.driver = driver;
    }

}
