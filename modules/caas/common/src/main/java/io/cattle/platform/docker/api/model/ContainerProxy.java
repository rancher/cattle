package io.cattle.platform.docker.api.model;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false, name = "containerProxy")
public interface ContainerProxy {

    public enum Scheme {
       http,
       https,
    }

    @Field(defaultValue = "80", min = 0)
    Integer getPort();

    @Field(defaultValue = "http")
    Scheme getScheme();

}
