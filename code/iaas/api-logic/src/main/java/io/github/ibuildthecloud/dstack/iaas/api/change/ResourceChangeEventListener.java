package io.github.ibuildthecloud.dstack.iaas.api.change;

import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedEventListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface ResourceChangeEventListener extends AnnotatedEventListener {

    @EventHandler
    public void stateChange(Event event);

    @EventHandler
    public void apiChange(Event event);

}