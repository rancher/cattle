package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;
import io.github.ibuildthecloud.gdapi.model.FieldType;

@Type(list = false)
public interface VolumeActivateInput {

    @Field(type = FieldType.REFERENCE, typeString = "reference[host]", nullable = true)
    Long getHostId();

}