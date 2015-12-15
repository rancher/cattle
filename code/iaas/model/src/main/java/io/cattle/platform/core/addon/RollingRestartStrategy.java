package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.HashMap;
import java.util.Map;

@Type(list = false, parent = "serviceUpgradeStrategy")
public class RollingRestartStrategy extends ServiceUpgradeStrategy {
    Map<Long, Long> instanceToStartCount = new HashMap<>();

    public Map<Long, Long> getInstanceToStartCount() {
        return instanceToStartCount;
    }

    public void setInstanceToStartCount(Map<Long, Long> instanceToStartCount) {
        this.instanceToStartCount = instanceToStartCount;
    }
}
