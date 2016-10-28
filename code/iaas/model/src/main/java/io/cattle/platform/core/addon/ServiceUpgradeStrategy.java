package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ServiceUpgradeStrategy {

    Long intervalMillis;
    Long batchSize;

    public ServiceUpgradeStrategy(Long intervalMillis, Long batchSize) {
        super();
        this.intervalMillis = intervalMillis;
        this.batchSize = batchSize;
    }

    public ServiceUpgradeStrategy() {
        super();
    }

    @Field(nullable = false, defaultValue = "2000", min = 100)
    public Long getIntervalMillis() {
        return intervalMillis;
    }

    public void setIntervalMillis(Long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    @Field(nullable = false, defaultValue = "1", min = 1)
    public Long getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Long batchSize) {
        this.batchSize = batchSize;
    }
}
