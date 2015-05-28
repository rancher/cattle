package io.cattle.platform.docker.api.model;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false, name = "containerLogs")
public interface ContainerLogs {

    @Field(defaultValue = "true")
    boolean getFollow();

    @Field(defaultValue = "100", min = 0)
    Integer getLines();
}
