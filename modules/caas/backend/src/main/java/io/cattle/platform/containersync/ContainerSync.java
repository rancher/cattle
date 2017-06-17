package io.cattle.platform.containersync;

import static io.cattle.platform.core.constants.InstanceConstants.*;

import io.cattle.platform.containersync.model.ContainerEventEvent;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.object.util.DataAccessor;

public interface ContainerSync extends AnnotatedEventListener {

    @EventHandler
    void containerEvent(ContainerEventEvent event);

    static boolean isNativeDockerStart(ProcessState state) {
        return DataAccessor.fromMap(state.getData()).withKey(PROCESS_DATA_NO_OP).withDefault(false).as(Boolean.class);
    }

}
