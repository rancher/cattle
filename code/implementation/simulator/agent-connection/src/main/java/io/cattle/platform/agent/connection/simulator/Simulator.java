package io.cattle.platform.agent.connection.simulator;

import io.cattle.platform.eventing.model.Event;

public interface Simulator {

    Event execute(Event event);

}
