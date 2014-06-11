package io.cattle.platform.eventing.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.retry.Retry;
import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventListener;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.RetryCallback;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.metrics.util.MetricsUtil;
import io.cattle.platform.pool.PoolConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
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
    Map<String,Counter> request = new ConcurrentHashMap<String, Counter>();
    Map<String,Counter> publish = new ConcurrentHashMap<String, Counter>();
    Map<String,Counter> failed = new ConcurrentHashMap<String, Counter>();
    Map<String,Timer> timers = new ConcurrentHashMap<String, Timer>();

    @Override
    public boolean publish(Event event) {
        return publish(event, false);
    }

    protected boolean publish(Event event, boolean request) {
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
            getEventLogOut().debug(eventString);
            increment(event, request);
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
    public ListenableFuture<Event> call(final Event event, EventCallOptions options) {
        final long start = System.currentTimeMillis();
        Integer retries = options.getRetry();
        Long timeoutMillis = options.getTimeoutMillis();
        final RetryCallback retryCallback = options.getRetryCallback();

        if ( event.getTimeoutMillis() != null ) {
            timeoutMillis = event.getTimeoutMillis();
        }

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

        final EventVO<Object> request = new EventVO<Object>(event, listener.getReplyTo());
        request.setTimeoutMillis(timeoutMillis);

        Retry retry = new Retry(retries, timeoutMillis, future, new Runnable() {
            @Override
            public void run() {
                Event requestToSend = null;
                if ( retryCallback == null ) {
                    requestToSend = request;
                } else {
                    requestToSend = retryCallback.beforeRetry(request);
                    if ( requestToSend == null ) {
                        requestToSend = request;
                    }
                }

                publish(requestToSend, true);
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
                    time(event, start);
                } catch (ExecutionException t) {
                    error(event);
                    if ( t.getCause() instanceof TimeoutException ) {
                        // Ignore don't treat as a bad listener
                    } else {
                        listener.setFailed(true);
                    }
                } catch (Throwable t) {
                    error(event);
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

        publish(request, true);

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

    protected void increment(Event event, boolean request) {
        String metricName = metricName(event, request ? "request" : "publish");
        if ( metricName == null ) {
            return;
        }

        Map<String,Counter> counters = request ? this.request : publish;
        Counter counter = counters.get(metricName);
        if ( counter == null ) {
            counter = MetricsUtil.getRegistry().counter(metricName);
            counters.put(metricName, counter);
        }

        counter.inc();
    }

    protected void error(Event event) {
        String metricName = metricName(event, "failed");
        if ( metricName == null ) {
            return;
        }

        Counter counter = failed.get(metricName);
        if ( counter == null ) {
            counter = MetricsUtil.getRegistry().counter(metricName);
            failed.put(metricName, counter);
        }

        counter.inc();
    }

    protected void time(Event event, long start) {
        long duration = System.currentTimeMillis() - start;
        String metricName = metricName(event, "time");
        if ( metricName == null ) {
            return;
        }

        Timer timer = timers.get(metricName);
        if ( timer == null ) {
            timer = MetricsUtil.getRegistry().timer(metricName);
            timers.put(metricName, timer);
        }

        timer.update(duration, TimeUnit.MILLISECONDS);
    }

    protected String metricName(Event event, String prefix) {
        String name = event.getName();
        if ( name.startsWith(REPLY_PREFIX) ) {
            return null;
        }

        return "event." + prefix + "." + StringUtils.substringBefore(name, EVENT_SEP).replace('.', '_');
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
