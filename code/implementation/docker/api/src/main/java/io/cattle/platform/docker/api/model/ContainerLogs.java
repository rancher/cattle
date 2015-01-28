package io.cattle.platform.docker.api.model;

import io.github.ibuildthecloud.gdapi.annotation.Field;

public interface ContainerLogs {

    @Field()
    boolean getFollow();

    @Field()
    Integer getLines();
}
