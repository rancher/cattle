package io.github.ibuildthecloud.api.pubsub.subscribe;

import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.util.type.Priority;

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
    protected Future<?> subscribe(List<String> eventNames, EventListener listener, MessageWriter writer, AtomicBoolean disconnect,
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
