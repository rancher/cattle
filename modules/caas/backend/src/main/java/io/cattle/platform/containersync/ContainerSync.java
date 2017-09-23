package io.cattle.platform.containersync;

import io.cattle.platform.containersync.model.ContainerEventEvent;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;

public interface ContainerSync extends AnnotatedEventListener {

    @EventHandler
    void containerEvent(ContainerEventEvent event);

}
