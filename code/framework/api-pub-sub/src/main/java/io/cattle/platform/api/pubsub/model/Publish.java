package io.cattle.platform.api.pubsub.model;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;
import java.util.Map;

@Type(pluralName = "publish", create = true, list = false)
public interface Publish {

    @Field(create = true)
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
    Map<String, Object> getData();

    @Field(create = true)
    Long getTime();

    @Field(create = true)
    String getPublisher();

    @Field(create = true)
    String getTransitioning();

    @Field(create = true)
    Integer getTransitioningProgress();

    @Field(create = true)
    String getTransitioningMessage();

    @Field(create = true)
    String getTransitioningInternalMessage();

}