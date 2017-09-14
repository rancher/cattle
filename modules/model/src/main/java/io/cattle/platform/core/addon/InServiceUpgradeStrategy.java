package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(list = false, parent = "serviceUpgradeStrategy")
public class InServiceUpgradeStrategy extends ServiceUpgradeStrategy {
    Object launchConfig;
    List<Object> secondaryLaunchConfigs;
    Object previousLaunchConfig;
    List<Object> previousSecondaryLaunchConfigs;
    boolean startFirst;

    public InServiceUpgradeStrategy() {
    }

    public InServiceUpgradeStrategy(Object launchConfig, List<Object> secondaryLaunchConfigs,
            Object previousLaunchConfig, List<Object> previousSecondaryLaunchConfigs, boolean startFirst,
            Long intervalMillis, Long batchSize) {
        super(intervalMillis, batchSize);
        this.launchConfig = launchConfig;
        this.secondaryLaunchConfigs = secondaryLaunchConfigs;
        this.previousLaunchConfig = previousLaunchConfig;
        this.previousSecondaryLaunchConfigs = previousSecondaryLaunchConfigs;
        this.startFirst = startFirst;
    }

    public Object getLaunchConfig() {
        return launchConfig;
    }

    public void setLaunchConfig(Object launchConfig) {
        this.launchConfig = launchConfig;
    }

    public List<Object> getSecondaryLaunchConfigs() {
        return secondaryLaunchConfigs;
    }

    public void setSecondaryLaunchConfigs(List<Object> secondaryLaunchConfigs) {
        this.secondaryLaunchConfigs = secondaryLaunchConfigs;
    }

    public Object getPreviousLaunchConfig() {
        return previousLaunchConfig;
    }

    public void setPreviousLaunchConfig(Object previousLaunchConfig) {
        this.previousLaunchConfig = previousLaunchConfig;
    }

    public List<Object> getPreviousSecondaryLaunchConfigs() {
        return previousSecondaryLaunchConfigs;
    }

    public void setPreviousSecondaryLaunchConfigs(List<Object> previousSecondaryLaunchConfigs) {
        this.previousSecondaryLaunchConfigs = previousSecondaryLaunchConfigs;
    }

    @Field(nullable = false, defaultValue = "false")
    public boolean getStartFirst() {
        return startFirst;
    }

    public void setStartFirst(boolean startFirst) {
        this.startFirst = startFirst;
    }
}
