package io.cattle.platform.agent;

import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.model.Event;

import com.google.common.util.concurrent.ListenableFuture;

public interface RemoteAgent {

    long getAgentId();

    void publish(Event event);


    Event callSync(Event event);

    Event callSync(Event event, EventCallOptions options);

    Event callSync(Event event, long timeoutMillis);


    ListenableFuture<? extends Event> call(Event event);

    ListenableFuture<? extends Event> call(Event event, EventCallOptions options);

    ListenableFuture<? extends Event> call(Event event, long timeoutMillis);


    <T extends Event> T callSync(Event event, Class<T> reply);

    <T extends Event> T callSync(Event event, Class<T> reply, EventCallOptions options);

    <T extends Event> T callSync(Event event, Class<T> reply, long timeoutMillis);


    <T extends Event> ListenableFuture<T> call(Event event, Class<T> reply);

    <T extends Event> ListenableFuture<T> call(Event event, Class<T> reply, EventCallOptions options);

    <T extends Event> ListenableFuture<T> call(Event event, Class<T> reply, long timeoutMillis);

}
