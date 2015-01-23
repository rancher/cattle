package io.cattle.platform.docker.api.model;

import io.github.ibuildthecloud.gdapi.annotation.Field;

import java.util.List;

public interface ContainerLogs {

    @Field(defaultValue="true")
    boolean getFollow();
    
    @Field(defaultValue="100")
    int getLines();

}