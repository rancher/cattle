package io.github.ibuildthecloud.agent.server.connection;

import com.google.common.util.concurrent.ListenableFuture;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface AgentConnection {

    long getAgentId();

    String getUri();

    /**
     * The future that is returned is expected to allows complete, either success or failure.
     * It it up to the implementation of the AgentConnection to ensure that the the future
     * is cancelled or set to an except in the situation of a timeout.  In other words,
     * The AgentConnection is responsilbe for handling the timeout logic.
     *
     * @param event
     * @return
     */
    ListenableFuture<Event> execute(Event event);

    void close();

    boolean isOpen();

}
