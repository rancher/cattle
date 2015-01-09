package io.cattle.platform.eventing.impl;

import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;

import java.util.Random;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class ListenerPoolObjectFactory implements PooledObjectFactory<FutureEventListener> {

    String prefix = Event.REPLY_PREFIX;
    EventService eventService;
    Random random = new Random();

    public ListenerPoolObjectFactory() {
    }

    public ListenerPoolObjectFactory(EventService eventService) {
        super();
        this.eventService = eventService;
    }

    @Override
    public PooledObject<FutureEventListener> makeObject() throws Exception {
        String key = "reply." + Math.abs(random.nextLong());
        FutureEventListener listener = new FutureEventListener(eventService, key);
        Future<?> future = eventService.subscribe(key, listener);
        future.get();
        return new DefaultPooledObject<FutureEventListener>(listener);
    }

    @Override
    public void destroyObject(PooledObject<FutureEventListener> p) throws Exception {
        eventService.unsubscribe(p.getObject());
    }

    @Override
    public boolean validateObject(PooledObject<FutureEventListener> p) {
        return !p.getObject().isFailed();
    }

    @Override
    public void activateObject(PooledObject<FutureEventListener> p) throws Exception {
        p.getObject().reset();
    }

    @Override
    public void passivateObject(PooledObject<FutureEventListener> p) throws Exception {
        p.getObject().reset();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

}
