package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class DependsOn {

    public enum DependsOnCondition {
        running,
        healthy
    };

    String service;
    DependsOnCondition condition;

    @Field(nullable = false)
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Field(defaultValue = "healthy")
    public DependsOnCondition getCondition() {
        return condition;
    }

    public void setCondition(DependsOnCondition condition) {
        this.condition = condition;
    }

}
