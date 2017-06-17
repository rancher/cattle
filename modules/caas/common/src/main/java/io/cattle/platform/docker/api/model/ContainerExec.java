package io.cattle.platform.docker.api.model;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(list=false)
public interface ContainerExec {

    @Field(defaultValue = "true")
    boolean getAttachStdin();

    @Field(defaultValue = "true")
    boolean getAttachStdout();

    @Field(defaultValue = "true")
    boolean getTty();

    @Field(required = true)
    List<String> getCommand();

}
