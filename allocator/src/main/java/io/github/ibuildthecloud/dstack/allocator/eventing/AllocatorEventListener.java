package io.github.ibuildthecloud.dstack.allocator.eventing;

import io.github.ibuildthecloud.dstack.eventing.annotation.AnnotatedEventListener;
import io.github.ibuildthecloud.dstack.eventing.annotation.EventHandler;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

public interface AllocatorEventListener extends AnnotatedEventListener {

    @EventHandler
    void instanceAllocate(Event event);

    @EventHandler
    void volumeAllocate(Event event);

    @EventHandler
    void instanceDeallocate(Event event);

}
