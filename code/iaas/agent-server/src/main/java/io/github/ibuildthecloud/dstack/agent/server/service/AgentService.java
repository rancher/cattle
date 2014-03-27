package io.github.ibuildthecloud.dstack.agent.server.service;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface AgentService {

    void execute(Event event);

}
