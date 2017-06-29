package io.cattle.platform.eventing;

import io.cattle.platform.eventing.model.Event;

public interface EventListener {

    void onEvent(Event event);

}
