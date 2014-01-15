package io.github.ibuildthecloud.api.pubsub.subscribe;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.async.retry.CancelRetryException;
import io.github.ibuildthecloud.dstack.async.retry.Retry;
import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.core.command.PingCommand;
import io.github.ibuildthecloud.dstack.core.event.CoreEvents;
import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;

public class NonBlockingSubscriptionHandler implements SubscriptionHandler {

    private static final Logger log = LoggerFactory.getLogger(NonBlockingSubscriptionHandler.class);

    public static final DynamicLongProperty API_SUB_PING_INVERVAL = ArchaiusUtil.getLong("api.sub.ping.interval.millis");
    public static final DynamicIntProperty API_MAX_PINGS = ArchaiusUtil.getInt("api.sub.max.pings");

    JsonMapper jsonMapper;
    EventService eventService;
    RetryTimeoutService retryTimeout;
    ExecutorService executorService;

    public NonBlockingSubscriptionHandler(JsonMapper jsonMapper, EventService eventService,
            RetryTimeoutService retryTimeout, ExecutorService executorService) {
        super();
        this.jsonMapper = jsonMapper;
        this.eventService = eventService;
        this.retryTimeout = retryTimeout;
        this.executorService = executorService;
    }

    @Override
    public boolean subscribe(List<String> eventNames, ApiRequest apiRequest, final boolean strip) throws IOException {
        final Object writeLock = new Object();
        final OutputStream os = getOutputStream(apiRequest);
        final AtomicBoolean disconnect = new AtomicBoolean(false);

        EventListener listener = new EventListener() {
            @Override
            public void onEvent(Event event) {
                try {
                    write(event, os, writeLock, strip);
                } catch (IOException e) {
                    log.trace("IOException on write to client for pub sub, disconnecting", e);
                    disconnect.set(true);
                }
            }
        };

        return subscribe(eventNames, listener, os, disconnect, writeLock, strip) != null;
    }

    protected OutputStream getOutputStream(ApiRequest apiRequest) throws IOException {
        return apiRequest.getOutputStream();
    }

    protected void write(Event event, OutputStream os, Object writeLock, boolean strip) throws IOException {
        EventVO newEvent = new EventVO(event);
        if ( strip ) {
            String name = newEvent.getName();
            if ( name != null ) {
                newEvent.setName(StringUtils.substringBefore(name, CoreEvents.EVENT_SEP));
            }
        }

        String content = jsonMapper.writeValueAsString(newEvent);
        write(os, content, writeLock);
    }

    protected void write(OutputStream os, String content, Object writeLock) throws IOException {
        synchronized (writeLock) {
            try {
                os.write((content + "\n").getBytes("UTF-8"));
                os.flush();
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    protected Future<?> subscribe(List<String> eventNames, EventListener listener, OutputStream os, AtomicBoolean disconnect,
            Object writeLock, boolean strip) {
        boolean unsubscribe = false;
        try {
            for ( String eventName : eventNames ) {
                eventService.subscribe(eventName, listener).get(API_SUB_PING_INVERVAL.get(), TimeUnit.MILLISECONDS);
            }
            write(new PingCommand(), os, writeLock, strip);
            return schedulePing(listener, os, disconnect, writeLock, strip);
        } catch (Throwable e) {
            unsubscribe = true;
        } finally {
            if ( unsubscribe ) {
                unsubscribe(disconnect, listener);
            }
        }

        return null;
    }

    protected Future<?> schedulePing(final EventListener listener, final OutputStream os,
            final AtomicBoolean disconnect, final Object writeLock, final boolean strip) {
        final SettableFuture<?> future = SettableFuture.create();
        retryTimeout.submit(new Retry(API_MAX_PINGS.get(), API_SUB_PING_INVERVAL.get(), future, new Runnable() {
            @Override
            public void run() {
                if ( disconnect.get() ) {
                    unsubscribe(disconnect, listener);
                    future.setException(new CancelRetryException());
                    throw new CancelRetryException();
                }
                try {
                    write(new PingCommand(), os, writeLock, strip);
                } catch (IOException e) {
                    log.debug("Got exception on write, disconnecting [{}]", e.getMessage());
                    unsubscribe(disconnect, listener);
                    future.setException(e);
                    throw new CancelRetryException();
                }
            }
        }));

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    unsubscribe(disconnect, listener);
                } catch (ExecutionException e) {
                    unsubscribe(disconnect, listener);
                }
            }
        }, executorService);

        return future;
    }

    protected void unsubscribe(AtomicBoolean disconnect, EventListener listener) {
        disconnect.set(true);
        eventService.unsubscribe(listener);
    }
}
