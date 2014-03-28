package io.cattle.platform.archaius.eventing;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;

public interface ArchaiusEventListener extends AnnotatedEventListener {

    @EventHandler
    void apiChange(Event event);
}
