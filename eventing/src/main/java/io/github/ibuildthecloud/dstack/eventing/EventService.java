package io.github.ibuildthecloud.dstack.eventing;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

import java.io.IOException;
import java.util.concurrent.Future;

public interface EventService {

    boolean publish(Event event);

    Future<?> subscribe(String eventName, EventListener listener);

    void unsubscribe(String eventName, EventListener listener);

    void unsubscribe(EventListener listener);

    Event call(Event event) throws IOException;

    Event call(Event event, int retry, long timeoutMillis) throws IOException;

    Future<Event> callAsync(Event event);

    Future<Event> callAsync(Event event, int retry, long timeoutMillis);

}
