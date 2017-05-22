package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public interface ServiceRollback {

    @Field(typeString = "reference[revision]")
    Long getRevisionId();

}
