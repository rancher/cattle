package io.cattle.platform.iaas.api.process;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.model.ProcessInstance;
import io.cattle.platform.engine.eventing.EngineEvents;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class ProcessInstanceReplayHandler implements ActionHandler {

    @Inject
    ObjectManager objectManager;
    @Inject
    EventService eventService;

    @Override
    public String getName() {
        return "processinstance.replay";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        ProcessInstance pi = objectManager.loadResource(ProcessInstance.class, request.getId());
        if (pi == null) {
            return null;
        }

        pi.setRunAfter(null);
        pi.setExecutionCount(0L);
        objectManager.persist(pi);

        Event event = EventVO.newEvent(EngineEvents.PROCESS_EXECUTE).withResourceId(pi.getId().toString());
        eventService.publish(event);

        return pi;
    }

}
