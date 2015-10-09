package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Type(list = false)
public class ServiceUpgrade {

    InServiceUpgradeStrategy inServiceStrategy;

    ToServiceUpgradeStrategy toServiceStrategy;

    @Field(nullable = true)
    public InServiceUpgradeStrategy getInServiceStrategy() {
        return inServiceStrategy;
    }

    public void setInServiceStrategy(InServiceUpgradeStrategy inService) {
        this.inServiceStrategy = inService;
    }

    @Field(nullable = true)
    public ToServiceUpgradeStrategy getToServiceStrategy() {
        return toServiceStrategy;
    }

    public void setToServiceStrategy(ToServiceUpgradeStrategy toService) {
        this.toServiceStrategy = toService;
    }

    @JsonIgnore
    public ServiceUpgradeStrategy getStrategy() {
        if (inServiceStrategy != null) {
            return inServiceStrategy;
        } else {
            return toServiceStrategy;
        }
    }

}
