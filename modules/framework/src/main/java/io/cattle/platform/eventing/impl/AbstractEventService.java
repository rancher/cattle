package io.cattle.platform.eventing.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.retry.Retry;
import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventListener;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.RetryCallback;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.pool.PoolConfig;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public abstract class AbstractEventService implements EventService {

    private static final Logger log = LoggerFactory.getLogger(AbstractEventService.class);
    private static final Logger EVENT_LOG_IN = LoggerFactory.getLogger("EventLogIn");
    private static final Logger EVENT_LOG_OUT = LoggerFactory.getLogger("EventLogOut");

    public static final DynamicIntProperty DEFAULT_RETRIES = ArchaiusUtil.getInt("eventing.retry");
    public static final DynamicLongProperty DEFAULT_TIMEOUT = ArchaiusUtil.getLong("eventing.timeout.millis");

    private static final Object SUBSCRIPTION_LOCK = new Object();

    RetryTimeoutService timeoutService;
    private ExecutorService executorService;
    Map<String, List<EventListener>> eventToListeners = new HashMap<>();
    Map<EventListener, Set<String>> listenerToEvents = new HashMap<>();
    JsonMapper jsonMapper;
    ObjectPool<FutureEventListener> listenerPool;

    public AbstractEventService(RetryTimeoutService timeoutService, ExecutorService executorService, JsonMapper jsonMapper) {
        super();
        this.timeoutService = timeoutService;
        this.executorService = executorService;
        this.jsonMapper = jsonMapper;
        init();
    }

    @Override
    public boolean publish(Event event) {
        return publish(event, false);
    }

    protected boolean publish(Event event, boolean request) {
        if (event == null) {
            return false;
        }

        String eventString = null;
        try {
            eventString = jsonMapper.writeValueAsString(event);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to marshall event [" + event + "] to string", e);
        }

        if (event.getName() == null) {
            log.error("Can not publish an event with a null name : {}", eventString);
            return false;
        }

        try {
            getEventLogOut().debug(eventString);
            return doPublish(event.getName(), event, eventString);
        } catch (Throwable e) {
            log.warn("Failed to publish event [" + eventString + "]", e);
            return false;
        }
    }

    protected abstract boolean doPublish(String name, Event event, String eventString) throws IOException;

    protected List<EventListener> getEventListeners(Event event) {
        String eventName = event.getName();
        List<EventListener> result = eventToListeners.get(eventName);

        if (event instanceof EventVO) {
            String listenerKey = ((EventVO<?, ?>) event).getListenerKey();
            if (listenerKey != null && !listenerKey.equals(eventName)) {
                List<EventListener> additional = eventToListeners.get(((EventVO<?, ?>) event).getListenerKey());
                if (additional != null) {
                    if (result == null) {
                        return additional;
                    } else {
                        result = new ArrayList<>(result);
                        result.addAll(additional);
                    }
                }
            }
        }

        return result;
    }

    protected Logger getEventLogIn() {
        return EVENT_LOG_IN;
    }

    protected Logger getEventLogOut() {
        return EVENT_LOG_OUT;
    }

    protected boolean register(String eventName, EventListener listener) {
        synchronized (SUBSCRIPTION_LOCK) {
            boolean doSubscribe = false;

            Set<String> events = listenerToEvents.get(listener);

            if (events == null) {
                events = new HashSet<>();
                listenerToEvents.put(listener, events);
            }

            List<EventListener> listeners = eventToListeners.get(eventName);

            if (listeners == null) {
                listeners = new CopyOnWriteArrayList<>();
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

            if (events != null) {
                events.remove(eventName);
                if (events.size() == 0) {
                    listenerToEvents.remove(listener);
                }
            }

            List<EventListener> listeners = eventToListeners.get(eventName);

            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.size() == 0) {
                    eventToListeners.remove(eventName);
                    doUnsubscribe = true;
                }
            }

            return doUnsubscribe;
        }
    }

    protected Set<String> getSubscriptions(EventListener eventListener) {
        synchronized (SUBSCRIPTION_LOCK) {
            Set<String> result = new HashSet<>();
            Set<String> current = listenerToEvents.get(eventListener);
            if (current != null) {
                result.addAll(current);
            }
            return result;
        }
    }

    @Override
    public ListenableFuture<?> subscribe(final String eventName, final EventListener listener) {
        final SettableFuture<?> future = SettableFuture.create();
        boolean doSubscribe = register(eventName, listener);

        if (doSubscribe) {
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
        if (doUnsubscribe) {
            doUnsubscribe(eventName);
        }

    }

    protected abstract void doUnsubscribe(String eventName);

    @Override
    public void unsubscribe(EventListener listener) {
        for (String eventName : getSubscriptions(listener)) {
            unsubscribe(eventName, listener);
        }
    }

    @Override
    public ListenableFuture<Event> call(final Event event, EventCallOptions options) {
        Integer retries = options.getRetry();
        Long timeoutMillis = options.getTimeoutMillis();
        final RetryCallback retryCallback = options.getRetryCallback();

        if (event.getTimeoutMillis() != null) {
            timeoutMillis = event.getTimeoutMillis();
        }

        if (retries == null) {
            retries = DEFAULT_RETRIES.get();
        }

        if (timeoutMillis == null) {
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

        final EventVO<Object, Object> request = new EventVO<>(event, listener.getReplyTo());
        request.setTimeoutMillis(timeoutMillis);

        Retry retry = new Retry(retries, timeoutMillis, future, new Runnable() {
            @Override
            public void run() {
                Event requestToSend = null;
                if (retryCallback == null) {
                    requestToSend = request;
                } else {
                    try {
                        requestToSend = retryCallback.beforeRetry(request);
                        if (requestToSend == null) {
                            requestToSend = request;
                        }
                    } catch (Throwable t) {
                        future.setException(t);
                    }
                }

                publish(requestToSend, true);
            }
        });

        final Object cancel = timeoutService.submit(retry);

        listener.setProgress(options.getProgress());
        listener.setFuture(future);
        listener.setEvent(request);

        if (options.isProgressIsKeepAlive() && options.getProgress() != null) {
            listener.setRetry(retry);
        }

        future.addListener(() -> {
            try {
                timeoutService.completed(cancel);
                future.get();
            } catch (ExecutionException t) {
                if (t.getCause() instanceof TimeoutException) {
                    // Ignore don't treat as a bad listener
                } else if (t.getCause() instanceof EventExecutionException) {
                    // Ignore event errors
                } else {
                    listener.setFailed(true);
                }
            } catch (Throwable t) {
                listener.setFailed(true);
            } finally {
                try {
                    listenerPool.returnObject(listener);
                } catch (Exception e) {
                    log.error("Failed to return object to pool [" + listener + "]", e);
                }
            }
        }, executorService);

        try {
            publish(request, true);
        } catch (Throwable t) {
            future.setException(t);
        }

        return future;
    }

    @PostConstruct
    private void init() {
        if (listenerPool == null) {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            PoolConfig.setConfig(config, "eventing.reply.pool", "eventing.reply.pool.", "global.pool.");
            listenerPool = new GenericObjectPool<>(new ListenerPoolObjectFactory(this), config);
        }
    }

    boolean isSubscribed(String eventName) {
        return eventToListeners.containsKey(eventName);
    }

    protected abstract void disconnect();

    protected Object getSubscriptionLock() {
        return SUBSCRIPTION_LOCK;
    }

}
