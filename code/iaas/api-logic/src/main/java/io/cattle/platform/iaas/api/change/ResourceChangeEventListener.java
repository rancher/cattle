package io.cattle.platform.iaas.api.change;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;

public interface ResourceChangeEventListener extends AnnotatedEventListener {

    @EventHandler
    public void stateChange(Event event);

    @EventHandler
    public void apiChange(Event event);

    @EventHandler
    public void resourceProgress(Event event);

    @EventHandler
    public void serviceEvent(Event event);

}