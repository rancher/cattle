package io.github.ibuildthecloud.dstack.eventing.impl;

import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

import java.util.Random;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.commons.pool.PoolableObjectFactory;

public class ListenerPoolObjectFactory implements PoolableObjectFactory<FutureEventListener> {

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
    public FutureEventListener makeObject() throws Exception {
        String key = "reply." + Math.abs(random.nextLong());
        FutureEventListener listener = new FutureEventListener(eventService, key);
        Future<?> future = eventService.subscribe(key, listener);
        future.get();
        return listener;
    }

    @Override
    public void destroyObject(FutureEventListener obj) throws Exception {
        eventService.unsubscribe(obj);
    }

    @Override
    public boolean validateObject(FutureEventListener obj) {
        return obj.isFailed();
    }

    @Override
    public void activateObject(FutureEventListener obj) throws Exception {
        obj.reset();
    }

    @Override
    public void passivateObject(FutureEventListener obj) throws Exception {
        obj.reset();
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
