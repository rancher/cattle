package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ServiceUpgrade {

    boolean updateLinks;
    String toServiceId;
    Long finalScale;
    Long intervalMillis;
    Long batchSize;

    public boolean isUpdateLinks() {
        return updateLinks;
    }

    public void setUpdateLinks(boolean updateLinks) {
        this.updateLinks = updateLinks;
    }

    @Field(typeString = "reference[service]", nullable = false, required = true)
    public String getToServiceId() {
        return toServiceId;
    }

    public void setToServiceId(String toServiceId) {
        this.toServiceId = toServiceId;
    }

    @Field(nullable = false, required = true, min = 1)
    public Long getFinalScale() {
        return finalScale;
    }

    public void setFinalScale(Long finalScale) {
        this.finalScale = finalScale;
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
