package io.github.ibuildthecloud.api.pubsub.model;

import java.util.List;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(pluralName = "publish", create = true, list = false)
public interface Publish {

    @Field(create = true, required = true)
    String getId();

    @Field(create = true, required = true)
    String getName();

    @Field(create = true, nullable = true)
    String getResourceId();

    @Field(create = true, nullable = true)
    String getResourceType();

    @Field(create = true)
    List<String> getPreviousIds();

    @Field(create = true, nullable = true)
    Object getData();

    @Field(create = true, required = true)
    Long getTime();

    @Field(create = true)
    String getPublisher();

}