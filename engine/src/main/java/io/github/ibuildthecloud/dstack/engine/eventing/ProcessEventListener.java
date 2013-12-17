package io.github.ibuildthecloud.dstack.engine.eventing;

import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface ProcessEventListener extends AnnotatedListener {

    @EventHandler(name = EngineEvents.PROCESS_EXECUTE)
    void processExecute(Event event);

}
