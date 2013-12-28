package io.github.ibuildthecloud.dstack.archaius.eventing;

import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedEventListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface ArchaiusEventListener extends AnnotatedEventListener {

    @EventHandler
    void apiChange(Event event);
}
