package io.cattle.platform.extension.dynamic.api.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ExternalHandlerProcessConfig {
    String name;
    String onError;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOnError() {
        return onError;
    }

    public void setOnError(String onError) {
        this.onError = onError;
    }
}
