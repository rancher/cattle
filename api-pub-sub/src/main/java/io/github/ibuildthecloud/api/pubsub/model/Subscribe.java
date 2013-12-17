package io.github.ibuildthecloud.api.pubsub.model;

import io.github.ibuildthecloud.gdapi.annotation.Type;


@Type(pluralName = "subscribe")
public interface Subscribe {

    String getEventName();

    Long getAgentId();

}
