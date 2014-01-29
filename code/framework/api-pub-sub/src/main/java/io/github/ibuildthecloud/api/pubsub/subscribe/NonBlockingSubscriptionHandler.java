package io.github.ibuildthecloud.api.pubsub.subscribe;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.async.retry.CancelRetryException;
import io.github.ibuildthecloud.dstack.async.retry.Retry;
import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.framework.command.PingCommand;
import io.github.ibuildthecloud.dstack.framework.event.FrameworkEvents;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

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
    boolean supportGet = false;

    public NonBlockingSubscriptionHandler() {
    }

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
        if ( Method.GET.isMethod(apiRequest.getMethod()) && ! supportGet ) {
            return false;
        }

        final Object writeLock = new Object();
        final MessageWriter writer = getMessageWriter(apiRequest);
        final AtomicBoolean disconnect = new AtomicBoolean(false);
        final IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();

        if ( writer == null ) {
            return false;
        }

        EventListener listener = new EventListener() {
            @Override
            public void onEvent(Event event) {
                try {
                    EventVO<?> obfuscated = new EventVO<Object>(event);
                    if ( obfuscated.getResourceType() == null ) {
                        obfuscated.setResourceId(null);
                    } else {
                        String id = idFormatter.formatId(obfuscated.getResourceType(), obfuscated.getResourceId());
                        obfuscated.setResourceId(id);
                    }

                    write(obfuscated, writer, writeLock, strip);
                } catch (IOException e) {
                    log.trace("IOException on write to client for pub sub, disconnecting", e);
                    disconnect.set(true);
                }
            }
        };

        return subscribe(eventNames, listener, writer, disconnect, writeLock, strip) != null;
    }

    protected MessageWriter getMessageWriter(ApiRequest apiRequest) throws IOException {
        return new OutputStreamMessageWriter(apiRequest.getOutputStream());
    }

    protected void write(Event event, MessageWriter writer, Object writeLock, boolean strip) throws IOException {
        EventVO<Object> newEvent = new EventVO<Object>(event);
        if ( strip ) {
            String name = newEvent.getName();
            if ( name != null ) {
                newEvent.setName(StringUtils.substringBefore(name, FrameworkEvents.EVENT_SEP));
            }
        }

        String content = jsonMapper.writeValueAsString(newEvent);
        write(writer, content, writeLock);
    }

    protected void write(MessageWriter writer, String content, Object writeLock) throws IOException {
        writer.write(content, writeLock);
    }

    protected Future<?> subscribe(List<String> eventNames, EventListener listener, MessageWriter writer, AtomicBoolean disconnect,
            Object writeLock, boolean strip) {
        boolean unsubscribe = false;
        try {
            for ( String eventName : eventNames ) {
                eventService.subscribe(eventName, listener).get(API_SUB_PING_INVERVAL.get(), TimeUnit.MILLISECONDS);
            }
            write(new PingCommand(), writer, writeLock, strip);
            return schedulePing(listener, writer, disconnect, writeLock, strip);
        } catch (Throwable e) {
            unsubscribe = true;
        } finally {
            if ( unsubscribe ) {
                unsubscribe(disconnect, listener);
            }
        }

        return null;
    }

    protected Future<?> schedulePing(final EventListener listener, final MessageWriter writer,
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
                    write(new PingCommand(), writer, writeLock, strip);
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

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public RetryTimeoutService getRetryTimeout() {
        return retryTimeout;
    }

    @Inject
    public void setRetryTimeout(RetryTimeoutService retryTimeout) {
        this.retryTimeout = retryTimeout;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Inject
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public boolean isSupportGet() {
        return supportGet;
    }

    public void setSupportGet(boolean supportGet) {
        this.supportGet = supportGet;
    }
}
