package io.cattle.platform.eventing;

import io.cattle.platform.eventing.model.Event;

public interface EventProgress {

    void progress(Event event);

}
