package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class RestartPolicy {

    String name;
    int maximumRetryCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaximumRetryCount() {
        return maximumRetryCount;
    }

    public void setMaximumRetryCount(int maximumRetryCount) {
        this.maximumRetryCount = maximumRetryCount;
    }

}
