package io.cattle.platform.allocator.eventing;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;

public interface AllocatorEventListener extends AnnotatedEventListener {

    @EventHandler
    void instanceAllocate(Event event);

    @EventHandler
    void volumeAllocate(Event event);

    @EventHandler
    void instanceDeallocate(Event event);

    @EventHandler
    void volumeDeallocate(Event event);

}
