package io.cattle.platform.eventing;

import io.cattle.platform.eventing.model.Event;

public interface EventCallback {

    void onEvent(Event response, Throwable t);

}
