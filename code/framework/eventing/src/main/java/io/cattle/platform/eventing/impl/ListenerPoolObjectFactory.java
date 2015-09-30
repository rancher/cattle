package io.cattle.platform.eventing.impl;

import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.eventing.model.Event;

import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class ListenerPoolObjectFactory implements PooledObjectFactory<FutureEventListener> {

    String prefix = Event.REPLY_PREFIX;
    AbstractEventService eventService;
    Random random = new Random();

    public ListenerPoolObjectFactory() {
    }

    public ListenerPoolObjectFactory(AbstractEventService eventService) {
        super();
        this.eventService = eventService;
    }

    @Override
    public PooledObject<FutureEventListener> makeObject() throws Exception {
        String key = prefix + Math.abs(random.nextLong());
        FutureEventListener listener = new FutureEventListener(eventService, key);
        Future<?> future = eventService.subscribe(key, listener);
        AsyncUtils.get(future, 10, TimeUnit.SECONDS);
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

    public AbstractEventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(AbstractEventService eventService) {
        this.eventService = eventService;
    }

}
