package io.github.ibuildthecloud.api.pubsub.manager;

import io.github.ibuildthecloud.api.pubsub.model.Publish;
import io.github.ibuildthecloud.api.pubsub.util.SubscriptionUtils;
import io.github.ibuildthecloud.api.pubsub.util.SubscriptionUtils.SubscriptionStyle;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.id.IdentityFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class PublishManager extends AbstractNoOpResourceManager {

    EventService eventService;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Publish.class };
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        Publish publish = request.proxyRequestObject(Publish.class);

        Event event = createEvent(publish);

        if ( SubscriptionUtils.getSubscriptionStyle(ApiUtils.getPolicy()) != SubscriptionStyle.RAW &&
                ! event.getName().startsWith(Event.REPLY_PREFIX) ) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }

        eventService.publish(event);

        return publish;
    }

    protected Event createEvent(Publish publish) {
        EventVO<Object> event = new EventVO<Object>();

        event.setId(publish.getId());
        event.setName(publish.getName());
        event.setResourceId(getResourceId(publish.getResourceId()));
        event.setResourceType(publish.getResourceType());
        event.setData(publish.getData());
        event.setPublisher(publish.getPublisher());
        event.setTransitioning(publish.getTransitioning());
        event.setTransitioningInternalMessage(publish.getTransitioningInternalMessage());
        event.setTransitioningMessage(event.getTransitioningMessage());
        event.setTransitioningProgress(event.getTransitioningProgress());

        event.setTime(new Date(publish.getTime()));

        List<String> previous = publish.getPreviousIds();
        if ( previous != null ) {
            event.setPreviousIds(previous.toArray(new String[previous.size()]));
        }

        return event;
    }

    protected String getResourceId(String resourceId) {
        IdFormatter formatter = ApiContext.getContext().getIdFormatter();
        return formatter.parseId(resourceId);
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

}