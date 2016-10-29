package io.cattle.platform.core.addon;

import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

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

    public boolean isFullUpgrade() {
        boolean primaryUpgrade = this.launchConfig != null && this.previousLaunchConfig != null;
        boolean isEmptySec = this.secondaryLaunchConfigs == null || this.secondaryLaunchConfigs.isEmpty();
        boolean isEmptyPrevSesc = this.previousSecondaryLaunchConfigs == null
                || this.previousSecondaryLaunchConfigs.isEmpty();

        boolean allSecondaryUpgrades = (isEmptySec == isEmptyPrevSesc)
                && (isEmptySec || this.secondaryLaunchConfigs.size() == this.previousSecondaryLaunchConfigs.size());
        return primaryUpgrade && allSecondaryUpgrades;
    }

    public Map<String, Pair<String, Map<String, Object>>> getNameToVersionToConfig(String svcName,
            boolean previous) {
        List<Object> lcs = new ArrayList<>();
        if (previous) {
            if (this.getPreviousSecondaryLaunchConfigs() != null) {
                lcs.addAll(this.getPreviousSecondaryLaunchConfigs());
            }
            lcs.add(this.getPreviousLaunchConfig());
        } else {
            if (this.getSecondaryLaunchConfigs() != null) {
                lcs.addAll(this.getSecondaryLaunchConfigs());
            }
            lcs.add(this.getLaunchConfig());
        }
        Map<String, Pair<String, Map<String, Object>>> lcNames = new HashMap<>();
        for (Object lc : lcs) {
            Map<String, Object> mapped = CollectionUtils.toMap(lc);
            Object name = mapped.get("name");
            if (name != null) {
                lcNames.put(name.toString(), Pair.of(mapped.get("version").toString(), mapped));
            } else {
                // primary config doesn't have the name set
                lcNames.put(svcName, Pair.of(mapped.get("version").toString(), mapped));
            }
        }
        return lcNames;
    }
}
