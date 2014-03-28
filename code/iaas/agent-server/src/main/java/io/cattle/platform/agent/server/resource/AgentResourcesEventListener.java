package io.cattle.platform.agent.server.resource;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.framework.event.Ping;

public interface AgentResourcesEventListener extends AnnotatedEventListener {

    @EventHandler
    void pingReply(Ping ping);

}
