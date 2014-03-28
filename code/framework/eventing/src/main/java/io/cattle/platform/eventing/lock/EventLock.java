package io.cattle.platform.eventing.lock;

import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class EventLock extends AbstractLockDefinition {

    public EventLock(Event event) {
        super("EVENT." + event.getId());
    }

}
