package io.github.ibuildthecloud.dstack.eventing;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface EventCallback {

    void onEvent(Event response, Throwable t);

}
