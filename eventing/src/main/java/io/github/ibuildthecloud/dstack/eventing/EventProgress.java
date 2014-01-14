package io.github.ibuildthecloud.dstack.eventing;

import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface EventProgress {

    void progress(Event event);

}
