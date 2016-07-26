package io.cattle.platform.eventing.exception;

import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.util.exception.ExecutionException;

import java.lang.reflect.InvocationTargetException;

public class EventExecutionException extends ExecutionException {

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

    @Override
    public String getTransitioningInternalMessage() {
        return event == null ? super.getTransitioningInternalMessage() : event.getTransitioningInternalMessage();
    }

    public static EventExecutionException fromEvent(Event event) {
        String internal = event.getTransitioningInternalMessage();
        if (internal != null && internal.startsWith("class:")) {
            String className = internal.split(":")[1];
            try {
                Class<?> clz = Class.forName(className);
                if (EventExecutionException.class.isAssignableFrom(clz)) {
                    if (event instanceof EventVO) {
                        ((EventVO<?>) event).setTransitioningInternalMessage(null);
                    }
                    return (EventExecutionException) clz.getConstructor(String.class, Event.class)
                            .newInstance(event.getTransitioningMessage(), event);
                }
            } catch (ClassNotFoundException e) {
            } catch (InvocationTargetException e) {
            } catch (NoSuchMethodException e) {
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            }
        }

        String message = event.getTransitioningInternalMessage() == null ?
                event.getTransitioningMessage() : event.getTransitioningInternalMessage();
        return new EventExecutionException(message, event);
    }

}
