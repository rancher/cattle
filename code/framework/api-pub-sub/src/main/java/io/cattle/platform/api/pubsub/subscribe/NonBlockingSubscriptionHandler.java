package io.cattle.platform.api.pubsub.subscribe;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.retry.CancelRetryException;
import io.cattle.platform.async.retry.Retry;
import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.eventing.EventListener;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.util.Collection;
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
    List<ApiPubSubEventPostProcessor> eventProcessors;

    public NonBlockingSubscriptionHandler() {
    }

    public NonBlockingSubscriptionHandler(JsonMapper jsonMapper, EventService eventService,
            RetryTimeoutService retryTimeout, ExecutorService executorService,
            List<ApiPubSubEventPostProcessor> eventProcessors) {
        super();
        this.jsonMapper = jsonMapper;
        this.eventService = eventService;
        this.retryTimeout = retryTimeout;
        this.executorService = executorService;
        this.eventProcessors = eventProcessors;
    }

    @Override
    public boolean subscribe(Collection<String> eventNames, ApiRequest apiRequest, final boolean strip) throws IOException {
        if ( Method.GET.isMethod(apiRequest.getMethod()) && ! supportGet ) {
            return false;
        }

        ApiContext apiContext = ApiContext.getContext();

        final Object writeLock = new Object();
        final MessageWriter writer = getMessageWriter(apiRequest);
        final AtomicBoolean disconnect = new AtomicBoolean(false);

        final ApiRequest request = new ApiRequest(apiRequest);
        final IdFormatter idFormatter = apiContext.getIdFormatter();
        final Object policy = apiContext.getPolicy();

        if ( writer == null ) {
            return false;
        }

        EventListener listener = new EventListener() {
            @Override
            public void onEvent(Event event) {
                try {
                    EventVO<Object> modified = new EventVO<Object>(event);

                    postProcess(modified, idFormatter, request, policy);
                    obfuscateIds(modified, idFormatter);

                    write(modified, writer, writeLock, strip);
                } catch (IOException e) {
                    log.trace("IOException on write to client for pub sub, disconnecting", e);
                    disconnect.set(true);
                }
            }
        };

        return subscribe(eventNames, listener, writer, disconnect, writeLock, strip) != null;
    }

    protected void postProcess(EventVO<Object> event, IdFormatter idFormatter, ApiRequest request, Object policy) {
        try {
            ApiContext context = ApiContext.newContext();
            context.setApiRequest(request);
            context.setIdFormatter(idFormatter);
            context.setPolicy(policy);

            for ( ApiPubSubEventPostProcessor processor : eventProcessors ) {
                processor.processEvent(event);
            }
        } finally {
            ApiContext.remove();
        }
    }

    protected void obfuscateIds(EventVO<?> event, IdFormatter idFormatter) {
        if ( event.getResourceType() == null ) {
            event.setResourceId(null);
        } else {
            String id = idFormatter.formatId(event.getResourceType(), event.getResourceId());
            event.setResourceId(id);
        }
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

    protected Future<?> subscribe(Collection<String> eventNames, EventListener listener, MessageWriter writer, AtomicBoolean disconnect,
            Object writeLock, boolean strip) {
        boolean unsubscribe = false;
        try {
            for ( String eventName : eventNames ) {
                eventService.subscribe(eventName, listener).get(API_SUB_PING_INVERVAL.get(), TimeUnit.MILLISECONDS);
            }
            write(new Ping(), writer, writeLock, strip);
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
                    write(new Ping(), writer, writeLock, strip);
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

    public List<ApiPubSubEventPostProcessor> getEventProcessors() {
        return eventProcessors;
    }

    @Inject
    public void setEventProcessors(List<ApiPubSubEventPostProcessor> eventProcessors) {
        this.eventProcessors = eventProcessors;
    }

}
