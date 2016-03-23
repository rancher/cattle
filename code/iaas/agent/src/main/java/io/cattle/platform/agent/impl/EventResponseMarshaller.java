package io.cattle.platform.agent.impl;

import io.cattle.platform.eventing.model.Event;

public interface EventResponseMarshaller {

    <T> T convert(Event resultEvent, Class<T> reply);

}
