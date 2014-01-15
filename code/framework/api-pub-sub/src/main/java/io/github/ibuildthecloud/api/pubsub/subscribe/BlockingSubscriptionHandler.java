package io.github.ibuildthecloud.api.pubsub.subscribe;

import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.json.JsonMapper;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockingSubscriptionHandler extends NonBlockingSubscriptionHandler {

    public BlockingSubscriptionHandler(JsonMapper jsonMapper, EventService eventService,
            RetryTimeoutService retryTimeout, ExecutorService executorService) {
        super(jsonMapper, eventService, retryTimeout, executorService);
    }

    @Override
    protected Future<?> subscribe(List<String> eventNames, EventListener listener, OutputStream os, AtomicBoolean disconnect,
            Object writeLock, boolean strip) {

        Future<?> future = super.subscribe(eventNames, listener, os, disconnect, writeLock, strip);
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

}
