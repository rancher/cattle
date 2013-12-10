package io.github.ibuildthecloud.dstack.eventing;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface EventListener {

    void onEvent(Event event);

}
