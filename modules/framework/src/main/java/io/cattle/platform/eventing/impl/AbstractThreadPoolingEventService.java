package io.cattle.platform.eventing.impl;

import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.eventing.EventListener;
import io.cattle.platform.eventing.PoolSpecificListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public abstract class AbstractThreadPoolingEventService extends AbstractEventService {

    private static final Logger log = LoggerFactory.getLogger(AbstractThreadPoolingEventService.class);

    ExecutorService runExecutorService;
    String threadCountSetting = "eventing.pool.%s.count";
    String defaultPoolName = EventHandler.DEFAULT_POOL_KEY;
    Map<String, ExecutorService> executorServices = new HashMap<>();

    public AbstractThreadPoolingEventService(RetryTimeoutService timeoutService, ExecutorService executorService, JsonMapper jsonMapper) {
        super(timeoutService, executorService, jsonMapper);
        this.runExecutorService = executorService;
    }

    protected void onEvent(String listenerKey, String eventName, byte[] bytes) {
        try {
            EventVO<?> event = jsonMapper.readValue(bytes, EventVO.class);
            if (eventName != null) {
                event.setName(eventName);
            }
            event.setListenerKey(listenerKey);

            onEvent(event);
        } catch (IOException e) {
            try {
                log.warn("Failed to unmarshall event [{}]", new String(bytes, "UTF-8"), e);
            } catch (UnsupportedEncodingException e1) {
                log.warn("Failed to unmarshall event [*bytes*]", e);
            }
        }
    }

    protected void onEvent(String listenerKey, String eventName, String eventString) {
        getEventLogIn().debug(eventString);

        try {
            EventVO<?> event = jsonMapper.readValue(eventString, EventVO.class);
            if (eventName != null) {
                event.setName(eventName);
            }
            event.setListenerKey(listenerKey);

            onEvent(event);
        } catch (IOException e) {
            log.warn("Failed to unmarshall event [{}]", eventString, e);
        }
    }

    protected void onEvent(final Event event) {
        new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                onEventInContext(event);
            }
        }.run();
    }

    protected void onEventInContext(Event event) {
        String name = event.getName();
        if (name == null) {
            log.debug("null event name on event [{}]", event);
            return;
        }

        List<EventListener> listeners = getEventListeners(event);
        if (listeners == null || listeners.size() == 0) {
            log.debug("No listeners found for [{}]", event.getName());
            return;
        }

        for (EventListener listener : listeners) {
            Executor executor = getExecutor(event, listener);
            Runnable runnable = getRunnable(event, listener);

            try {
                executor.execute(runnable);
            } catch (RejectedExecutionException e) {
                log.debug("Too busy to process [{}]", event);
            }
        }
    }

    protected Runnable getRunnable(final Event event, final EventListener listener) {
        return new NoExceptionRunnable() {
            @Override
            protected void doRun() throws Exception {
                try {
                    listener.onEvent(event);
                } catch (FailedToAcquireLockException e) {
                    log.trace("Failed to acquire lock on event [{}], this is probably normal", event, e);
                }
            }
        };
    }

    protected Executor getExecutor(Event event, EventListener listener) {
        Executor executor = null;
        if (listener instanceof PoolSpecificListener) {
            executor = executorServices.get(((PoolSpecificListener) listener).getPoolKey());
        }

        String eventName = event.getName();
        if (executor == null && eventName != null) {
            if (eventName.startsWith(Event.REPLY_PREFIX)) {
                executor = executorServices.get("reply");
            } else if (eventName.endsWith(Event.REPLY_SUFFIX)) {
                executor = executorServices.get("reply");
            }
        }

        if (executor == null) {
            executor = getDefaultExecutor();
        }

        return executor;
    }

    protected Executor getDefaultExecutor() {
        return runExecutorService;
    }

    public String getThreadCountSetting() {
        return threadCountSetting;
    }

    public void setThreadCountSetting(String threadCountSetting) {
        this.threadCountSetting = threadCountSetting;
    }

    public String getDefaultPoolName() {
        return defaultPoolName;
    }

    public void setDefaultPoolName(String defaultPoolName) {
        this.defaultPoolName = defaultPoolName;
    }

}
