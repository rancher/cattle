package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ServiceRestart {
    RollingRestartStrategy rollingRestartStrategy;

    @Field(nullable = false, required = true)
    public RollingRestartStrategy getRollingRestartStrategy() {
        return rollingRestartStrategy;
    }

    public void setRollingRestartStrategy(RollingRestartStrategy rollingRestartStrategy) {
        this.rollingRestartStrategy = rollingRestartStrategy;
    }
}
