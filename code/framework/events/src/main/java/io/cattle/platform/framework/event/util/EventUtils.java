package io.cattle.platform.framework.event.util;

import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;

import java.util.Map;

public class EventUtils {

    public static void triggerStateChanged(EventService eventService, String resourceId, String resourceType,
            Map<String, Object> data) {
        Event event = EventVO.newEvent(FrameworkEvents.STATE_CHANGE)
                .withData(data)
                .withResourceType(resourceType)
                .withResourceId(resourceId.toString());

        eventService.publish(event);
    }
}
