package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false, parent = "serviceUpgradeStrategy")
public class ToServiceUpgradeStrategy extends ServiceUpgradeStrategy {
    boolean updateLinks;
    String toServiceId;
    Long finalScale;

    public boolean isUpdateLinks() {
        return updateLinks;
    }

    public void setUpdateLinks(boolean updateLinks) {
        this.updateLinks = updateLinks;
    }

    @Field(typeString = "reference[service]")
    public String getToServiceId() {
        return toServiceId;
    }

    public void setToServiceId(String toServiceId) {
        this.toServiceId = toServiceId;
    }

    @Field(min = 1, defaultValue = "1")
    public Long getFinalScale() {
        return finalScale;
    }

    public void setFinalScale(Long finalScale) {
        this.finalScale = finalScale;
    }

}
