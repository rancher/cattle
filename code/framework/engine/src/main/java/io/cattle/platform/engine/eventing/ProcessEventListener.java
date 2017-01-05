package io.cattle.platform.engine.eventing;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;

public interface ProcessEventListener extends AnnotatedEventListener {

    @EventHandler(name = EngineEvents.PROCESS_EXECUTE)
    void processExecute(Event event);

}
