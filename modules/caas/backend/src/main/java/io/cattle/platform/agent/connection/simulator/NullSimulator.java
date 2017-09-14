package io.cattle.platform.agent.connection.simulator;

import io.cattle.platform.eventing.model.Event;

public class NullSimulator implements Simulator {

    @Override
    public Event execute(Event event) {
        return null;
    }

}
