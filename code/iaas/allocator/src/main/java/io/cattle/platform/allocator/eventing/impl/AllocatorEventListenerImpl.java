package io.cattle.platform.allocator.eventing.impl;

import io.cattle.platform.allocator.eventing.AllocatorEventListener;
import io.cattle.platform.allocator.exception.AllocationException;
import io.cattle.platform.allocator.service.AllocationRequest;
import io.cattle.platform.allocator.service.Allocator;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocatorEventListenerImpl implements AllocatorEventListener {

    private static final Logger log = LoggerFactory.getLogger(AllocatorEventListenerImpl.class);

    @Inject
    Allocator allocator;
    @Inject
    EventService eventService;

    @Override
    public void instanceAllocate(Event event) {
        allocate(event);
    }

    @Override
    public void instanceDeallocate(Event event) {
        deallocate(event);
    }

    @Override
    public void volumeAllocate(Event event) {
        allocate(event);
    }

    @Override
    public void volumeDeallocate(Event event) {
        deallocate(event);
    }

    protected void allocate(Event event) {
        log.info("Allocating [{}:{}]", event.getResourceType(), event.getResourceId());

        AllocationRequest request = new AllocationRequest(event);

        try {
            allocator.allocate(request);
            log.info("Allocator [{}] handled request [{}]", allocator, request);
            if (request.isSendReply()) {
                eventService.publish(EventVO.reply(event));
            }
        } catch (AllocationException e) {
            String errorMessage = "Scheduling failed: " + e.getMessage();
            eventService.publish(EventVO.reply(event).withTransitioningMessage(errorMessage).withTransitioning(Event.TRANSITIONING_ERROR));
        }
    }

    protected void deallocate(Event event) {
        log.info("Deallocating [{}:{}]", event.getResourceType(), event.getResourceId());

        AllocationRequest request = new AllocationRequest(event);

        try {
            allocator.deallocate(request);
            log.info("Deallocator [{}] handled request [{}]", allocator, request);
            if (request.isSendReply()) {
                eventService.publish(EventVO.reply(event));
            }
        } catch (AllocationException e) {
            String errorMessage = "Failed to deallocate: " + e.getMessage();
            eventService.publish(EventVO.reply(event).withTransitioningMessage(errorMessage).withTransitioning(Event.TRANSITIONING_ERROR));
        }
    }
}
