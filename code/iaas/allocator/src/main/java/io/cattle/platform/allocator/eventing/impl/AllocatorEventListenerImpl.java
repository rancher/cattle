package io.cattle.platform.allocator.eventing.impl;

import io.cattle.platform.allocator.eventing.AllocatorEventListener;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.service.AllocationRequest;
import io.cattle.platform.allocator.service.Allocator;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.exception.FailedToAllocateEventException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;

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
        String errorMessage = "Failed to find a placement";
        boolean handled = false;

        try {
            for (Allocator allocator : allocators) {
                if (allocator.allocate(request)) {
                    handled = true;
                    log.info("Allocator [{}] handled request [{}]", allocator, request);
                    break;
                }
            }
        } catch (FailedToAllocate e) {
            errorMessage = "Scheduling failed: " + e.getMessage();
        }

        if (handled) {
            if (request.isSendReply()) {
                eventService.publish(EventVO.reply(event));
            }
        } else {
            log.error("No allocator handled [{}]", event);
            if (FAIL_ON_NO_ALLOCATOR.get()) {
                eventService.publish(EventVO.replyWithException(event, FailedToAllocateEventException.class, errorMessage));
            }
        }
    }

    protected void deallocate(Event event) {
        log.info("Deallocating [{}:{}]", event.getResourceType(), event.getResourceId());

        AllocationRequest request = new AllocationRequest(event);
        boolean handled = false;

        for (Allocator allocator : allocators) {
            if (allocator.deallocate(request)) {
                handled = true;
                log.info("Deallocator [{}] handled request [{}]", allocator, request);
                break;
            }
        }

        if (handled && request.isSendReply()) {
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
