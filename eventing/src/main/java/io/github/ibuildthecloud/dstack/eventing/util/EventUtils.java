package io.github.ibuildthecloud.dstack.eventing.util;

import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;

public class EventUtils {

    public static Event newEvent(String name, String resourceId) {
        EventVO event = new EventVO();
        event.setName(name);
        event.setResourceId(resourceId);
        return event;
    }

}
