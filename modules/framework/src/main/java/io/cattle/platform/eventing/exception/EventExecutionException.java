package io.cattle.platform.eventing.exception;

import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.exception.LoggableException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;

public class EventExecutionException extends ExecutionException implements LoggableException {

    private static final long serialVersionUID = 3499233034118987175L;

    Event event;

    public EventExecutionException(String message, Event event) {
        super(message);
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }

    @Override
    public String getTransitioningMessage() {
        return event == null ? super.getTransitioningMessage() : event.getTransitioningMessage();
    }

    public static EventExecutionException fromEvent(Event event) {
        String internal = event.getTransitioningMessage();
        if (internal != null && internal.startsWith("class:")) {
            String[] parts = StringUtils.removeStart(internal, "class:").split(":", 2);
            if (parts.length == 2) {
                String className = parts[0];
                String message = parts[1];
                try {
                    Class<?> clz = Class.forName(className);
                    if (EventExecutionException.class.isAssignableFrom(clz)) {
                        return (EventExecutionException) clz.getConstructor(String.class, Event.class)
                                .newInstance(message, event);
                    }
                } catch (ClassNotFoundException e) {
                } catch (InvocationTargetException e) {
                } catch (NoSuchMethodException e) {
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                }
            }
        }

        return new EventExecutionException(event.getTransitioningMessage(), event);
    }

    @Override
    public void log(Logger log) {
        Object name = null;
        if (event != null) {
            name = event.getPreviousNames();
            if (name == null) {
                name = event.getName();
            }
        }
        log.error("Agent error for [{}]: {}", name, getMessage());
    }

}
