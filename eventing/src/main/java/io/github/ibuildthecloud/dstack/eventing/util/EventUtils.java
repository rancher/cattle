package io.github.ibuildthecloud.dstack.eventing.util;

import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;

public class EventUtils {

    public static EventVO reply(Event request) {
        String[] previousIds = request.getPreviousIds();
        if ( previousIds != null && previousIds.length > 0 ) {
            String[] newIds = new String[previousIds.length+1];
            System.arraycopy(previousIds, 0, newIds, 1, previousIds.length);
            newIds[0] = request.getId();

            previousIds = newIds;
        } else {
            previousIds = new String[] { request.getId() };
        }

        EventVO event = new EventVO();
        event.setName(request.getReplyTo());
        event.setPreviousIds(previousIds);
        event.setResourceId(request.getResourceId());
        event.setResourceType(request.getResourceType());

        return event;
    }

    public static Event newEvent(String name, String resourceId) {
        EventVO event = new EventVO();
        event.setName(name);
        event.setResourceId(resourceId);
        return event;
    }

}
