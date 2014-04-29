package io.cattle.platform.api.pubsub.subscribe;

import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.eventing.EventListener;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.Priority;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockingSubscriptionHandler extends NonBlockingSubscriptionHandler implements Priority {

    public BlockingSubscriptionHandler() {
    }

    public BlockingSubscriptionHandler(JsonMapper jsonMapper, EventService eventService,
            RetryTimeoutService retryTimeout, ExecutorService executorService,
            List<ApiPubSubEventPostProcessor> eventProcessors) {
        super(jsonMapper, eventService, retryTimeout, executorService, eventProcessors);
    }

    @Override
    protected Future<?> subscribe(Collection<String> eventNames, EventListener listener, MessageWriter writer, AtomicBoolean disconnect,
            Object writeLock, boolean strip) {

        Future<?> future = super.subscribe(eventNames, listener, writer, disconnect, writeLock, strip);
        if ( future == null ) {
            return null;
        }

        try {
            future.get();
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        }

        return future;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
