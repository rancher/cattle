package io.github.ibuildthecloud.dstack.eventing.impl;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.async.retry.Retry;
import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.async.utils.AsyncUtils;
import io.github.ibuildthecloud.dstack.eventing.EventCallOptions;
import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.exception.EventExecutionException;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.pool.PoolConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;

public abstract class AbstractEventService implements EventService {

    private static final Logger log = LoggerFactory.getLogger(AbstractEventService.class);
    private static final Logger eventLogIn = LoggerFactory.getLogger("EventLogIn");
    private static final Logger eventLogOut = LoggerFactory.getLogger("EventLogOut");


    private static final DynamicIntProperty DEFAULT_RETRIES = ArchaiusUtil.getInt("eventing.retry");
    private static final DynamicLongProperty DEFAULT_TIMEOUT = ArchaiusUtil.getLong("eventing.timeout.millis");

    private static final Object SUBSCRIPTION_LOCK = new Object();

    RetryTimeoutService timeoutService;
    ExecutorService executorService;
    Map<String, List<EventListener>> eventToListeners = new HashMap<String, List<EventListener>>();
    Map<EventListener, Set<String>> listenerToEvents = new HashMap<EventListener, Set<String>>();
    JsonMapper jsonMapper;
    ObjectPool<FutureEventListener> listenerPool;

    @Override
    public boolean publish(Event event) {
        if ( event == null ) {
            return false;
        }

        String eventString = null;
        try {
            eventString = jsonMapper.writeValueAsString(event);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to marshall event [" + event + "] to string", e);
        }

        if ( event.getName() == null ) {
            log.error("Can not publish an event with a null name : {}", eventString);
            return false;
        }

        try {
            getEventLogOut().debug("{} : {}", event.getName(), eventString);
            return doPublish(event.getName(), event, eventString);
        } catch ( Throwable e ) {
            log.warn("Failed to publish event [" + eventString + "]", e);
            return false;
        }
    }

    protected abstract boolean doPublish(String name, Event event, String eventString) throws IOException;

    protected List<EventListener> getEventListeners(Event event) {
        String eventName = event.getName();
        List<EventListener> result = eventToListeners.get(eventName);

        if ( event instanceof EventVO ) {
            String listenerKey = ((EventVO<?>)event).getListenerKey();
            if ( listenerKey != null && ! listenerKey.equals(eventName) ) {
                List<EventListener> additional = eventToListeners.get(((EventVO<?>)event).getListenerKey());
                if ( additional != null ) {
                    if ( result == null ) {
                        return additional;
                    } else {
                        result = new ArrayList<EventListener>(result);
                        result.addAll(additional);
                    }
                }
            }
        }

        return result;
    }

    protected Logger getEventLogIn() {
        return eventLogIn;
    }

    protected Logger getEventLogOut() {
        return eventLogOut;
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
    public ListenableFuture<?> subscribe(final String eventName, final EventListener listener) {
        final SettableFuture<?> future = SettableFuture.create();
        boolean doSubscribe = register(eventName, listener);

        if ( doSubscribe ) {
            doSubscribe(eventName, future);
        } else {
            future.set(null);
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

    protected EventCallOptions defaultCallOptions() {
        return new EventCallOptions()
            .withRetry(DEFAULT_RETRIES.get())
            .withTimeoutMillis(DEFAULT_TIMEOUT.get());
    }

    @Override
    public Event callSync(Event event) {
        return callSync(event, defaultCallOptions());
    }

    @Override
    public Event callSync(Event event, EventCallOptions options) {
        try {
            return AsyncUtils.get(call(event, options));
        } catch ( EventExecutionException e ) {
            /* This is done so that the exception will have a better stack trace.
             * Normally the exceptions from a future will have a pretty sparse stack
             * not giving too much context
             */
            throw new EventExecutionException(e);
        }
    }

    @Override
    public ListenableFuture<Event> call(Event event) {
        return call(event, defaultCallOptions());
    }

    @Override
    public ListenableFuture<Event> call(Event event, EventCallOptions options) {
        Integer retries = options.getRetry();
        Long timeoutMillis = options.getTimeoutMillis();

        if ( retries == null ) {
            retries = DEFAULT_RETRIES.get();
        }
        if ( timeoutMillis == null ) {
            timeoutMillis = DEFAULT_TIMEOUT.get();
        }

        final SettableFuture<Event> future = SettableFuture.create();
        final FutureEventListener listener;

        try {
            listener = listenerPool.borrowObject();
        } catch (Exception e) {
            future.setException(e);
            return future;
        }

        final Event request = new EventVO<Object>(event, listener.getReplyTo());
        Retry retry = new Retry(retries, timeoutMillis, future, new Runnable() {
            @Override
            public void run() {
                publish(request);
            }
        });

        final Object cancel = timeoutService.submit(retry);

        listener.setProgress(options.getProgress());
        listener.setFuture(future);
        listener.setEvent(request);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    timeoutService.completed(cancel);
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

        publish(request);

        return future;
    }

    @PostConstruct
    public void init() {
        if ( listenerPool == null ) {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            PoolConfig.setConfig(config, "eventing.reply.pool",
                    "eventing.reply.pool.",
                    "global.pool.");
            listenerPool = new GenericObjectPool<FutureEventListener>(new ListenerPoolObjectFactory(this), config);
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

    public ObjectPool<FutureEventListener> getListenerPool() {
        return listenerPool;
    }

    public void setListenerPool(ObjectPool<FutureEventListener> listenerPool) {
        this.listenerPool = listenerPool;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Inject
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public RetryTimeoutService getTimeoutService() {
        return timeoutService;
    }

    @Inject
    public void setTimeoutService(RetryTimeoutService timeoutService) {
        this.timeoutService = timeoutService;
    }

}
