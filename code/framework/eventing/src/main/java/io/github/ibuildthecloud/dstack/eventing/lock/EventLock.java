package io.github.ibuildthecloud.dstack.eventing.lock;

import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class EventLock extends AbstractLockDefinition {

    public EventLock(Event event) {
        super("EVENT." + event.getId());
    }

}
