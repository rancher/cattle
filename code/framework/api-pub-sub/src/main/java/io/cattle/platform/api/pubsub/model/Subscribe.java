package io.cattle.platform.api.pubsub.model;

import java.util.List;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(pluralName = "subscribe")
public interface Subscribe {

    @Field(create = true, minLength = 1, validChars = "*._0-9a-zA-Z;=")
    List<String> getEventNames();

    Long getAgentId();

}
