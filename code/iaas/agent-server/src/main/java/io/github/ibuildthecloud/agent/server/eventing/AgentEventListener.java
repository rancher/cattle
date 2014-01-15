package io.github.ibuildthecloud.agent.server.eventing;

import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedEventListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface AgentEventListener extends AnnotatedEventListener {

    @EventHandler
    void agentRequest(Event event);

}
