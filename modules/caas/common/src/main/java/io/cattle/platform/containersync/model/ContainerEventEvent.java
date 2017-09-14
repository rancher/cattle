package io.cattle.platform.containersync.model;

import io.cattle.platform.core.addon.ContainerEvent;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;

public class ContainerEventEvent extends EventVO<ContainerEvent, Object> {

    public ContainerEventEvent() {
        setName(FrameworkEvents.CONTAINER_EVENT);
    }

    public ContainerEventEvent(ContainerEvent data) {
        this();
        setData(data);
        setResourceType(ClusterConstants.TYPE);
        setResourceId(data.getClusterId().toString());
    }

}
