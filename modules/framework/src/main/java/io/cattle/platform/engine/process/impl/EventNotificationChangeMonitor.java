package io.cattle.platform.engine.process.impl;

import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.manager.impl.ProcessRecord;
import io.cattle.platform.engine.process.ProcessServiceContext;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.StateChangeMonitor;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Map;

public class EventNotificationChangeMonitor implements StateChangeMonitor {

    @Override
    public void onChange(boolean schedule, String previousState, String newState, ProcessRecord record, ProcessState state, ProcessServiceContext context) {
        Map<String, Object> data = CollectionUtils.asMap("previousState", previousState, "state", newState);
        addData(data, schedule, previousState, newState, record, state, context);

        Event event = makeEvent(record.getResourceType(), record.getResourceId()).withData(data);

        publish(schedule, event, context);
    }

    protected EventVO<Object> makeEvent(String resourceType, String resourceId) {
        return EventVO.newEvent(FrameworkEvents.STATE_CHANGE).withResourceType(resourceType).withResourceId(resourceId);

    }

    protected void publish(boolean schedule, Event event, ProcessServiceContext context) {
        if (schedule) {
            DeferredUtils.deferPublish(context.getEventService(), event);
        } else {
            context.getEventService().publish(event);
        }
    }

    public void addData(Map<String, Object> data, boolean schedule, String previousState, String newState, ProcessRecord record, ProcessState state,
            ProcessServiceContext context) {
    }

}
