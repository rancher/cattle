package io.cattle.platform.agent.server.service;

import io.cattle.platform.eventing.model.Event;

public interface AgentService {

    void execute(Event event);

}
