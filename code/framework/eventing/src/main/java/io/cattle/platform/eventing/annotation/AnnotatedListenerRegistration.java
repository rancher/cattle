package io.cattle.platform.eventing.annotation;

import io.cattle.platform.eventing.EventListener;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.util.EventUtils;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.util.type.InitializationTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class AnnotatedListenerRegistration implements InitializationTask {

    List<AnnotatedEventListener> listeners;
    EventService eventService;
    JsonMapper jsonMapper;
    LockManager lockManager;

    @Override
    public void start() {
        for (AnnotatedEventListener listener : listeners) {
            register(listener);
        }
    }

    protected void register(AnnotatedEventListener listener) {
        Map<String, EventListener> listeners = parseListener(listener);
        for (Map.Entry<String, EventListener> entry : listeners.entrySet()) {
            String eventName = entry.getKey();
            EventListener annotatedListener = entry.getValue();
            eventService.subscribe(eventName, annotatedListener);
        }
    }

    protected List<String> getEventNames(EventHandler handler, AnnotatedEventListener listener, Method method) {
        Class<? extends EventNameProvider> supplierClass = handler.nameProvider();

        if (supplierClass == EventNameProvider.class) {
            return Arrays.asList(EventUtils.getEventNameNonProvided(handler, listener, method));
        } else {
            EventNameProvider supplier = null;
            try {
                supplier = supplierClass.newInstance();
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Failed to instantiate [" + supplierClass + "]", e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Failed to instantiate [" + supplierClass + "]", e);
            }

            return supplier.events(handler, listener, method);
        }
    }

    protected Map<String, EventListener> parseListener(AnnotatedEventListener listener) {
        Map<String, EventListener> result = new LinkedHashMap<String, EventListener>();

        for (Class<?> clz : getClasses(listener.getClass())) {
            for (Method m : clz.getMethods()) {
                EventHandler h = m.getAnnotation(EventHandler.class);
                if (h == null) {
                    continue;
                }

                for (String eventName : getEventNames(h, listener, m)) {
                    result.put(eventName, new MethodInvokingListener(lockManager, jsonMapper, h, m, listener));
                }
            }
        }

        return result;
    }

    protected List<Class<?>> getClasses(Class<?> clz) {
        List<Class<?>> result = new ArrayList<Class<?>>();

        if (clz == null || clz.getPackage().getName().startsWith("java.lang")) {
            return result;
        }

        result.add(clz);
        for (Class<?> iface : clz.getInterfaces()) {
            result.addAll(getClasses(iface));
        }

        result.addAll(getClasses(clz.getSuperclass()));

        return result;
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

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

}
