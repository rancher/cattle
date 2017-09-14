package io.cattle.platform.api.change;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;

public interface ResourceChangeEventListener extends AnnotatedEventListener {

    @EventHandler
    void stateChange(Event event);

    @EventHandler
    void apiChange(Event event);

    @EventHandler
    void resourceProgress(Event event);

    @EventHandler
    void serviceEvent(Event event);

}