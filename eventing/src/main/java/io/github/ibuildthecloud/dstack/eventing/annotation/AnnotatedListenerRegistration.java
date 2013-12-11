package io.github.ibuildthecloud.dstack.eventing.annotation;

import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

public class AnnotatedListenerRegistration implements InitializationTask {

    List<AnnotatedListener> listeners;
    EventService eventService;

    public List<AnnotatedListener> getListeners() {
        return listeners;
    }

    @Inject
    public void setListeners(List<AnnotatedListener> listeners) {
        this.listeners = listeners;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public void start() {
        for ( AnnotatedListener listener : listeners ) {
            register(listener);
        }
    }

    protected void register(AnnotatedListener listener) {
        List<Future<?>> futures = new ArrayList<Future<?>>();

        Map<String,EventListener> listeners = parseListener(listener);
        for ( Map.Entry<String, EventListener> entry : listeners.entrySet() ) {
            String eventName = entry.getKey();
            EventListener annotatedListener = entry.getValue();
            futures.add(eventService.subscribe(eventName, annotatedListener));
        }

        for ( Future<?> future : futures ) {
            try {
                future.get();
            } catch (InterruptedException e) {
                throw new IllegalStateException("Failed to register listener");
            } catch (ExecutionException e) {
                throw new IllegalStateException("Failed to register listener", e.getCause());
            }
        }
    }

    protected Map<String, EventListener> parseListener(AnnotatedListener listener) {
        Map<String, EventListener> result = new LinkedHashMap<String, EventListener>();

        for ( Method m : listener.getClass().getMethods() ) {
            Handles h = m.getAnnotation(Handles.class);
            if ( h == null ) {
                continue;
            }

            result.put(h.name(), new MethodInvokingListener(m, listener));
        }

        return result;
    }

    @Override
    public void stop() {
    }

}
