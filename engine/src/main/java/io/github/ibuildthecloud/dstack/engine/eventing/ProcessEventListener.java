package io.github.ibuildthecloud.dstack.engine.eventing;

import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedEventListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface ProcessEventListener extends AnnotatedEventListener {

    @EventHandler(name = EngineEvents.PROCESS_EXECUTE)
    void processExecute(Event event);

}
