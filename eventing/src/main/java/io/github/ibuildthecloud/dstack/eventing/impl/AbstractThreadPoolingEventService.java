package io.github.ibuildthecloud.dstack.eventing.impl;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.PoolSpecificListener;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.lock.exception.FailedToAcquireLockException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractThreadPoolingEventService extends AbstractEventService {

    private static final Logger log = LoggerFactory.getLogger(AbstractThreadPoolingEventService.class);

    String threadCountSetting = "eventing.pool.%s.count";
    String defaultPoolName = "default";
    Map<String,Executor> executors = new ConcurrentHashMap<String, Executor>();
    Map<String,Executor> queuedExecutors = new ConcurrentHashMap<String, Executor>();

    protected void onEvent(String listenerKey, String eventName, byte[] bytes) {
        try {
            EventVO event = jsonMapper.readValue(bytes, EventVO.class);
            if ( eventName != null ) {
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
        getEventLogIn().debug("{} : {}", eventName, eventString);

        try {
            EventVO event = jsonMapper.readValue(eventString, EventVO.class);
            if ( eventName != null ) {
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
        if ( name == null ) {
            log.debug("null event name on event [{}]", event);
            return;
        }

        List<EventListener> listeners = getEventListeners(event);
        if ( listeners == null || listeners.size() == 0 ) {
            log.debug("No listeners found for [{}]", event.getName());
            return;
        }

        for ( EventListener listener : listeners ) {
            Executor executor = getExecutor(event, listener);
            Runnable runnable = getRunnable(event, listener);

            try {
                executor.execute(runnable);
            } catch ( RejectedExecutionException e ) {
                log.info("Too busy to process [{}]", event);
            }
        }
    }

    protected Runnable getRunnable(final Event event, final EventListener listener) {
        return new NoExceptionRunnable() {
            @Override
            protected void doRun() throws Exception {
                try {
                    listener.onEvent(event);
                } catch ( FailedToAcquireLockException e ) {
                    log.trace("Failed to acquire lock on event [{}], this is probably normal", event, e);
                }
            }
        };
    }

    protected Executor getExecutor(Event event, EventListener listener) {
        if ( listener instanceof PoolSpecificListener ) {
            return getExecutor(((PoolSpecificListener)listener).getPoolKey(),
                    ((PoolSpecificListener)listener).isAllowQueueing(),
                    ((PoolSpecificListener)listener).getQueueDepth());
        } else {
            return getDefaultExecutor();
        }
    }

    protected Executor getDefaultExecutor() {
        return getExecutor(defaultPoolName, false, 0);
    }

    protected Executor getExecutor(String name, boolean queued, int size) {
        Map<String,Executor> executors = queued ? queuedExecutors : this.executors;
        Executor executor = executors.get(name);

        if ( executor != null ) {
            return executor;
        }

        int threadCount = getThreadCount(name, defaultPoolName);
        if ( queued ) {
            executor = new ThreadPoolExecutor(threadCount, threadCount, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(size));
        } else {
            executor = new ThreadPoolExecutor(Math.min(5, threadCount), threadCount, 3L, TimeUnit.MINUTES, new SynchronousQueue<Runnable>());
        }

        executors.put(name, executor);

        return executor;
    }

    protected int getThreadCount(String name, String defaultSetting) {
        int count = ArchaiusUtil.getInt(String.format(threadCountSetting, name)).get();
        if ( count > 0 ) {
            return count;
        } else {
            return ArchaiusUtil.getInt(String.format(threadCountSetting, defaultSetting)).get();
        }
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
