package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class ServiceUpgrade {

    InServiceUpgradeStrategy inServiceStrategy;

    @Field(nullable = true, required = true)
    public InServiceUpgradeStrategy getInServiceStrategy() {
        return inServiceStrategy;
    }

    public void setInServiceStrategy(InServiceUpgradeStrategy inService) {
        this.inServiceStrategy = inService;
    }
}
