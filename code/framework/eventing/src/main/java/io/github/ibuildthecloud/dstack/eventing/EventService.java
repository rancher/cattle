package io.github.ibuildthecloud.dstack.eventing;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

import com.google.common.util.concurrent.ListenableFuture;

public interface EventService {

    public static final String EVENT_SEP = ";";
    public static final String REPLY_PREFIX = "reply.";

    boolean publish(Event event);

    ListenableFuture<?> subscribe(String eventName, EventListener listener);

    void unsubscribe(String eventName, EventListener listener);

    void unsubscribe(EventListener listener);

    Event callSync(Event event);

    Event callSync(Event event, EventCallOptions callOptions);

    ListenableFuture<Event> call(Event event);

    ListenableFuture<Event> call(Event event, EventCallOptions callOptions);

}
