package io.cattle.platform.eventing;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.eventing.model.Event;

public interface EventService {

    String EVENT_SEP = ";";
    String REPLY_PREFIX = "reply.";

    boolean publish(Event event);

    ListenableFuture<?> subscribe(String eventName, EventListener listener);

    void unsubscribe(String eventName, EventListener listener);

    void unsubscribe(EventListener listener);

    Event callSync(Event event, EventCallOptions callOptions);

    ListenableFuture<Event> call(Event event, EventCallOptions callOptions);

}
