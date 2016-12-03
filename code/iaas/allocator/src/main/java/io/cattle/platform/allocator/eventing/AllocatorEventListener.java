package io.cattle.platform.allocator.eventing;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;

public interface AllocatorEventListener extends AnnotatedEventListener {

    @EventHandler(poolKey="allocator")
    void instanceAllocate(Event event);

    @EventHandler(poolKey="allocator")
    void instanceDeallocate(Event event);
}
