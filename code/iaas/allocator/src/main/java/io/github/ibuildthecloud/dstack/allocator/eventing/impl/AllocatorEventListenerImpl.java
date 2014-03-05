package io.github.ibuildthecloud.dstack.allocator.eventing.impl;

import io.github.ibuildthecloud.dstack.allocator.eventing.AllocatorEventListener;
import io.github.ibuildthecloud.dstack.allocator.service.AllocationRequest;
import io.github.ibuildthecloud.dstack.allocator.service.Allocator;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class AllocatorEventListenerImpl implements AllocatorEventListener {

    private static final DynamicBooleanProperty FAIL_ON_NO_ALLOCATOR = ArchaiusUtil.getBoolean("allocator.fail.not.handled");
    private static final Logger log = LoggerFactory.getLogger(AllocatorEventListenerImpl.class);

    List<Allocator> allocators;
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
        boolean handled = false;

        for ( Allocator allocator : allocators ) {
            if ( allocator.allocate(request) ) {
                handled = true;
                log.info("Allocator [{}] handled request [{}]", allocator, request);
                break;
            }
        }

        if ( handled ) {
            if ( request.isSendReply() ) {
                eventService.publish(EventVO.reply(event));
            }
        } else {
            log.error("No allocator handled [{}]", event);
            if ( FAIL_ON_NO_ALLOCATOR.get() ) {
                eventService.publish(EventVO.reply(event)
                        .withTransitioningMessage("Failed to find a placement")
                        .withTransitioning(Event.TRANSITIONING_ERROR));
            }
        }
    }

    protected void deallocate(Event event) {
        log.info("Deallocating [{}:{}]", event.getResourceType(), event.getResourceId());

        AllocationRequest request = new AllocationRequest(event);
        boolean handled = false;

        for ( Allocator allocator : allocators ) {
            if ( allocator.deallocate(request) ) {
                handled = true;
                log.info("Deallocator [{}] handled request [{}]", allocator, request);
                break;
            }
        }

        if ( handled && request.isSendReply() ) {
            eventService.publish(EventVO.reply(event));
        }
    }



    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public List<Allocator> getAllocators() {
        return allocators;
    }

    @Inject
    public void setAllocators(List<Allocator> allocators) {
        this.allocators = allocators;
    }

}