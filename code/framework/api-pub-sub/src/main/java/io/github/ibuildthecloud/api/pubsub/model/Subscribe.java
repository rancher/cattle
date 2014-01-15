package io.github.ibuildthecloud.api.pubsub.model;

import java.util.List;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;


@Type(pluralName = "subscribe")
public interface Subscribe {

    @Field(create = true, required = true, minLength = 1, validChars = "*._0-1a-zA-Z")
    List<String> getEventNames();

    Long getAgentId();

}
