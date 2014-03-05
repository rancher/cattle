package io.github.ibuildthecloud.agent.server.resource;

import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedEventListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.framework.event.Ping;

public interface AgentResourcesEventListener extends AnnotatedEventListener {

    @EventHandler
    void pingReply(Ping ping);

}
