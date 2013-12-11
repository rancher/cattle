package io.github.ibuildthecloud.dstack.eventing.impl;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.pool.PoolConfig;
import io.github.ibuildthecloud.dstack.util.concurrent.DelayedObject;
import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;

public abstract class AbstractEventService implements EventService {

    private static final Logger log = LoggerFactory.getLogger(AbstractEventService.class);
    private static final Logger eventLog = LoggerFactory.getLogger("EventLog");


    private static final DynamicIntProperty DEFAULT_RETRIES = ArchaiusUtil.getIntProperty("eventing.retry");
    private static final DynamicLongProperty DEFAULT_TIMEOUT = ArchaiusUtil.getLongProperty("eventing.timeout.millis");

    private static final Object SUBSCRIPTION_LOCK = new Object();

    ExecutorService executorService;
    DelayQueue<DelayedObject<Retry>> retryQueue = new DelayQueue<DelayedObject<Retry>>();
    Map<String, List<EventListener>> eventToListeners = new HashMap<String, List<EventListener>>();
    Map<EventListener, Set<String>> listenerToEvents = new HashMap<EventListener, Set<String>>();
    JsonMapper jsonMapper;
    GenericObjectPool<FutureEventListener> listenerPool = new GenericObjectPool<FutureEventListener>(new ListenerPoolObjectFactory(this));

    @Override
    public boolean publish(Event event) {
        String eventString = null;
        try {
            eventString = jsonMapper.writeValueAsString(event);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to marshall event [" + event + "] to string", e);
        }
        try {
            getEventLog().debug("Out : ", event);
            doPublish(event.getName(), event, eventString);
        } catch ( IOException e ) {
            log.warn("Failed to publish event [" + eventString + "]", e);
            return false;
        }

        return true;
    }

    protected abstract void doPublish(String name, Event event, String eventString) throws IOException;

    protected List<EventListener> getEventListeners(String eventName) {
        return eventToListeners.get(eventName);
    }

    protected Logger getEventLog() {
        return eventLog;
    }

    protected boolean register(String eventName, EventListener listener) {
        synchronized (SUBSCRIPTION_LOCK) {
            boolean doSubscribe = false;

            Set<String> events = listenerToEvents.get(listener);

            if ( events == null ) {
                events = new HashSet<String>();
                listenerToEvents.put(listener, events);
            }

            List<EventListener> listeners = eventToListeners.get(eventName);

            if ( listeners == null ) {
                listeners = new CopyOnWriteArrayList<EventListener>();
                eventToListeners.put(eventName, listeners);
                doSubscribe = true;
            }

            listeners.add(listener);
            events.add(eventName);

            return doSubscribe;
        }
    }

    protected boolean unregister(String eventName, EventListener listener) {
        synchronized (SUBSCRIPTION_LOCK) {
            boolean doUnsubscribe = false;
            Set<String> events = listenerToEvents.get(listener);

            if ( events != null ) {
                events.remove(eventName);
                if ( events.size() == 0 ) {
                    listenerToEvents.remove(listener);
                }
            }

            List<EventListener> listeners = eventToListeners.get(eventName);

            if ( listeners != null ) {
                listeners.remove(listener);
                if ( listeners.size() == 0 ) {
                    eventToListeners.remove(eventName);
                    doUnsubscribe = true;
                }
            }

            return doUnsubscribe;
        }
    }

    protected Set<String> getSubscriptions(EventListener eventListener) {
        synchronized (SUBSCRIPTION_LOCK) {
            Set<String> result = new HashSet<String>();
            Set<String> current = listenerToEvents.get(eventListener);
            if ( current != null ) {
                result.addAll(current);
            }
            return result;
        }
    }

    @Override
    public Future<?> subscribe(final String eventName, final EventListener listener) {
        final SettableFuture<?> future = SettableFuture.create();
        boolean doSubscribe = register(eventName, listener);

        if ( doSubscribe ) {
            doSubscribe(eventName, future);
        }

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();
                } catch (Exception e) {
                    unsubscribe(eventName, listener);
                    disconnect();
                }
            }
        }, executorService);

        return future;
    }

    protected abstract void doSubscribe(String eventName, SettableFuture<?> future);

    @Override
    public void unsubscribe(String eventName, EventListener listener) {
        boolean doUnsubscribe = unregister(eventName, listener);
        if ( doUnsubscribe ) {
            doUnsubscribe(eventName);
        }

    }

    protected abstract void doUnsubscribe(String eventName);

    @Override
    public void unsubscribe(EventListener listener) {
        for ( String eventName : getSubscriptions(listener) ) {
            unsubscribe(eventName, listener);
        }
    }

    @Override
    public Event call(Event event) throws IOException {
        return call(event, DEFAULT_RETRIES.get(), DEFAULT_TIMEOUT.get());
    }

    @Override
    public Event call(Event event, int retry, long timeoutMillis) throws IOException {
        try {
            return callAsync(event, retry, timeoutMillis).get();
        } catch (InterruptedException e) {
            throw new IOException(e);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            ExceptionUtils.rethrow(t, IOException.class);
            ExceptionUtils.rethrowRuntime(t);
            throw new IOException(t);
        }
    }

    @Override
    public Future<Event> callAsync(Event event) {
        return callAsync(event, DEFAULT_RETRIES.get(), DEFAULT_TIMEOUT.get());
    }

    @Override
    public Future<Event> callAsync(Event event, int retries, long timeoutMillis) {
        final SettableFuture<Event> future = SettableFuture.create();
        final FutureEventListener listener;

        try {
            listener = listenerPool.borrowObject();
        } catch (Exception e) {
            future.setException(e);
            return future;
        }

        event = new EventVO(event, listener.getReplyTo());
        Retry retry = new Retry(retries, timeoutMillis, event, future);
        final DelayedObject<Retry> delayed = new DelayedObject<Retry>(System.currentTimeMillis() + timeoutMillis, retry);

        retryQueue.add(delayed);

        listener.setFuture(future);
        listener.setEvent(event);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    retryQueue.remove(delayed);
                    future.get();
                } catch (Throwable t) {
                    listener.setFailed(true);
                } finally {
                    try {
                        listenerPool.returnObject(listener);
                    } catch (Exception e) {
                        log.error("Failed to return object to pool [" + listener + "]", e);
                    }
                }
            }
        }, executorService);

        publish(event);

        return future;
    }

    @PostConstruct
    public void init() {
        PoolConfig.setConfig(listenerPool, "eventPool", "event.pool.");
    }

    public void retry() {
        DelayedObject<Retry> delayed = retryQueue.poll();
        while ( delayed != null ) {
            Retry retry = delayed.getObject();
            retry.retryCount++;

            if ( retry.retryCount >= retry.retries ) {
                retry.future.setException(new TimeoutException());
            } else {
                retryQueue.add(new DelayedObject<Retry>(System.currentTimeMillis() + retry.timeoutMillis, retry));
                publish(retry.event);
            }

            delayed = retryQueue.poll();
        }
    }

    protected abstract void disconnect();

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public GenericObjectPool<FutureEventListener> getListenerPool() {
        return listenerPool;
    }

    public void setListenerPool(GenericObjectPool<FutureEventListener> listenerPool) {
        this.listenerPool = listenerPool;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Inject
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    private static class Retry {
        int retryCount;
        int retries;
        Long timeoutMillis;
        Event event;
        SettableFuture<Event> future;

        public Retry(int retries, Long timeoutMillis, Event event, SettableFuture<Event> future) {
            super();
            this.retryCount = 0;
            this.retries = retries;
            this.timeoutMillis = timeoutMillis;
            this.event = event;
            this.future = future;
        }
    }

}
