package io.cattle.platform.agent.server.eventing;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;

public interface AgentEventListener extends AnnotatedEventListener {

    @EventHandler
    void agentRequest(Event event);

}
