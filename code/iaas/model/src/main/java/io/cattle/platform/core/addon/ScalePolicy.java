package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list=false)
/*
 * Remove this class after db migration for scale policy service to new global min/max/interval scale
 */
public class ScalePolicy {
    Integer increment;
    Integer min;
    Integer max;

    @Field(required = false, nullable = true, defaultValue = "1", min = 1)
    public Integer getIncrement() {
        return increment;
    }

    public void setIncrement(Integer increment) {
        this.increment = increment;
    }

    @Field(required = true, nullable = true, defaultValue = "1", min = 1)
    public Integer getMin() {
        return min;
    }

    public void setMin(Integer minScale) {
        this.min = minScale;
    }

    @Field(required = false, nullable = true, defaultValue = "1", min = 1)
    public Integer getMax() {
        return max;
    }

    public void setMax(Integer maxScale) {
        this.max = maxScale;
    }
}
