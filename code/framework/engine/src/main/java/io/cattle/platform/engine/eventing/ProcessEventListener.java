package io.cattle.platform.engine.eventing;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;

public interface ProcessEventListener extends AnnotatedEventListener {

    @EventHandler(name = EngineEvents.PROCESS_EXECUTE2)
    void processExecute(ProcessExecuteEvent event);

}
