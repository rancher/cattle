package io.github.ibuildthecloud.dstack.agent;

import com.google.common.util.concurrent.ListenableFuture;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface RemoteAgent {

    void publish(Event event);


    Event callSync(Event event);

    Event callSync(Event event, long timeout);


    ListenableFuture<Event> call(Event event);

    ListenableFuture<Event> call(Event event, long timeout);


    <T extends Event> T callSync(Event event, Class<T> reply);

    <T extends Event> T callSync(Event event, Class<T> reply, long timeout);


    <T extends Event> ListenableFuture<T> call(Event event, Class<T> reply);

    <T extends Event> ListenableFuture<T> call(Event event, Class<T> reply, long timeout);

}
