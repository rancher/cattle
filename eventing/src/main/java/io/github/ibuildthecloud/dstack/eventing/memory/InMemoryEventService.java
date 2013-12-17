package io.github.ibuildthecloud.dstack.eventing.memory;

import io.github.ibuildthecloud.dstack.eventing.impl.AbstractThreadPoolingEventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

import java.io.IOException;

import com.google.common.util.concurrent.SettableFuture;

public class InMemoryEventService extends AbstractThreadPoolingEventService {

    @Override
    protected boolean doPublish(final String name, Event event, final String eventString) throws IOException {
        getDefaultExecutor().execute(new Runnable() {
            @Override
            public void run() {
                onEvent(name, eventString);
            }
        });

        return true;
    }

    @Override
    protected void doSubscribe(String eventName, SettableFuture<?> future) {
        future.set(null);
    }

    @Override
    protected void doUnsubscribe(String eventName) {
    }

    @Override
    protected void disconnect() {
    }

}
