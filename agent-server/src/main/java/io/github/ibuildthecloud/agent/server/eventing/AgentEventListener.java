package io.github.ibuildthecloud.agent.server.eventing;

import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface AgentEventListener extends AnnotatedListener {

    @EventHandler
    void agentRequest(Event event);

}
