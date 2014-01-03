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
        event.setPreviousNames(prepend(request.getPreviousNames(), request.getName()));
        event.setPreviousIds(prepend(request.getPreviousIds(), request.getId()));
        event.setResourceId(request.getResourceId());
        event.setResourceType(request.getResourceType());

        return event;
    }

    protected static String[] prepend(String[] array, String value) {
        if ( array != null && array.length > 0 ) {
            String[] newIds = new String[array.length+1];
            System.arraycopy(array, 0, newIds, 1, array.length);
            newIds[0] = value;

            array = newIds;
        } else {
            array = new String[] { value };
        }

        return array;
    }

    public static EventVO newEventFromData(String name, Object data) {
        EventVO result = newEvent(name, null);
        result.setData(data);
        return result;
    }

    public static EventVO newEvent(String name, String resourceId) {
        EventVO event = new EventVO();
        event.setName(name);
        event.setResourceId(resourceId);
        return event;
    }

}
