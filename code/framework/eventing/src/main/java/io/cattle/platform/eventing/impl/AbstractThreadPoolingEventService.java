package io.cattle.platform.eventing.impl;

import io.cattle.platform.eventing.EventListener;
import io.cattle.platform.eventing.PoolSpecificListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.metrics.util.MetricsUtil;
import io.cattle.platform.util.concurrent.NamedExecutorService;
import io.cattle.platform.util.type.InitializationTask;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.codahale.metrics.Counter;

public abstract class AbstractThreadPoolingEventService extends AbstractEventService implements InitializationTask {

    private static final Logger log = LoggerFactory.getLogger(AbstractThreadPoolingEventService.class);

    String threadCountSetting = "eventing.pool.%s.count";
    String defaultPoolName = EventHandler.DEFAULT_POOL_KEY;
    List<NamedExecutorService> namedExecutorServiceList;
    Map<String,ExecutorService> executorServices;
    Map<String,Counter> dropped = new ConcurrentHashMap<String, Counter>();

    protected void onEvent(String listenerKey, String eventName, byte[] bytes) {
        try {
            EventVO<?> event = jsonMapper.readValue(bytes, EventVO.class);
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
        getEventLogIn().debug(eventString);

        try {
            EventVO<?> event = jsonMapper.readValue(eventString, EventVO.class);
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
                dropped(event);
                log.debug("Too busy to process [{}]", event);
            }
        }
    }

    protected void dropped(Event event) {
        String metricName = metricName(event, "dropped");
        if ( metricName == null ) {
            return;
        }

        Counter counter = dropped.get(metricName);
        if ( counter == null ) {
            counter = MetricsUtil.getRegistry().counter(metricName);
            dropped.put(metricName, counter);
        }

        counter.inc();
    }


    protected Runnable getRunnable(final Event event, final EventListener listener) {
        return new NoExceptionRunnable() {
            @Override
            protected void doRun() throws Exception {
                try {
                    Map<String,Object> context = event.getContext();
                    if ( context != null ) {
                        MDC.setContextMap(context);
                    }

                    listener.onEvent(event);
                } catch ( FailedToAcquireLockException e ) {
                    log.trace("Failed to acquire lock on event [{}], this is probably normal", event, e);
                }
            }
        };
    }

    protected Executor getExecutor(Event event, EventListener listener) {
        Executor executor = null;
        if ( listener instanceof PoolSpecificListener ) {
            executor = executorServices.get(((PoolSpecificListener)listener).getPoolKey());
        }

        if ( executor == null ) {
            executor = getDefaultExecutor();
        }

        return executor;
    }

    protected Executor getDefaultExecutor() {
        return executorService;
    }

    @Override
    public void start() {
        executorServices = new HashMap<String, ExecutorService>();
        for ( NamedExecutorService named : namedExecutorServiceList ) {
            executorServices.put(named.getName(), named.getExecutorService());
        }
    }

    @Override
    public void stop() {
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

    public List<NamedExecutorService> getNamedExecutorServiceList() {
        return namedExecutorServiceList;
    }

    public void setNamedExecutorServiceList(List<NamedExecutorService> namedExecutorServiceList) {
        this.namedExecutorServiceList = namedExecutorServiceList;
    }
}
