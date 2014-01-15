package io.github.ibuildthecloud.dstack.eventing.annotation;

import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;

public class AnnotatedListenerRegistration implements InitializationTask {

    List<AnnotatedEventListener> listeners;
    EventService eventService;

    @Override
    public void start() {
        for ( AnnotatedEventListener listener : listeners ) {
            register(listener);
        }
    }

    protected void register(AnnotatedEventListener listener) {
        Map<String,EventListener> listeners = parseListener(listener);
        for ( Map.Entry<String, EventListener> entry : listeners.entrySet() ) {
            String eventName = entry.getKey();
            EventListener annotatedListener = entry.getValue();
            eventService.subscribe(eventName, annotatedListener);
        }
    }

    protected String getEventName(EventHandler handler, Method method) {
        String name = handler.name();
        if ( StringUtils.isEmpty(name) ) {
            return NamedUtils.toDotSeparated(method.getName());
        } else {
            return name;
        }
    }

    protected Map<String, EventListener> parseListener(AnnotatedEventListener listener) {
        Map<String, EventListener> result = new LinkedHashMap<String, EventListener>();

        for ( Class<?> clz : getClasses(listener.getClass()) ) {
            for ( Method m : clz.getMethods() ) {
                EventHandler h = m.getAnnotation(EventHandler.class);
                if ( h == null ) {
                    continue;
                }

                String eventName = getEventName(h, m);
                result.put(eventName, new MethodInvokingListener(h, m, listener));
            }
        }

        return result;
    }

    protected List<Class<?>> getClasses(Class<?> clz) {
        List<Class<?>> result = new ArrayList<Class<?>>();

        if ( clz == null || clz.getPackage().getName().startsWith("java.lang") ) {
            return result;
        }

        result.add(clz);
        for ( Class<?> iface : clz.getInterfaces() ) {
            result.addAll(getClasses(iface));
        }

        result.addAll(getClasses(clz.getSuperclass()));

        return result;
    }

    @Override
    public void stop() {
    }

    public List<AnnotatedEventListener> getListeners() {
        return listeners;
    }

    @Inject
    public void setListeners(List<AnnotatedEventListener> listeners) {
        this.listeners = listeners;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

}
