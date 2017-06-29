package io.cattle.platform.eventing.util;

import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventProgress;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.util.type.NamedUtils;

import java.lang.reflect.Method;

import org.apache.commons.lang.StringUtils;

public class EventUtils {

    public static String getEventNameNonProvided(EventHandler handler, AnnotatedEventListener listener, Method method) {
        String name = handler.name();
        if (StringUtils.isEmpty(name)) {
            return NamedUtils.toDotSeparated(method.getName());
        } else {
            return name;
        }
    }

    public static EventProgress chainProgress(final Event previousEvent, final EventService eventService) {
        return new EventProgress() {
            @Override
            public void progress(Event event) {
                if (event.getTransitioning() == null || Event.TRANSITIONING_NO.equals(event.getTransitioning())) {
                    return;
                }

                EventVO<Object> reply = EventVO.reply(previousEvent);
                copyTransitioning(event, reply);

                eventService.publish(event);
            }
        };
    }

    public static EventCallOptions chainOptions(Event event) {
        return new EventCallOptions().withProgressIsKeepAlive(true).withRetry(0)
                .withTimeoutMillis(event.getTimeoutMillis());
    }

    public static boolean isTransitioningEvent(Event event) {
        if (event == null || event.getTransitioning() == null || Event.TRANSITIONING_NO.equals(event.getTransitioning())) {
            return false;
        }

        return true;
    }

    public static void copyTransitioning(Event from, EventVO<?> to) {
        to.setTransitioning(from.getTransitioning());
        to.setTransitioningInternalMessage(from.getTransitioningInternalMessage());
        to.setTransitioningMessage(from.getTransitioningMessage());
        to.setTransitioningProgress(from.getTransitioningProgress());
    }
}
