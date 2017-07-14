package io.cattle.platform.api.pubsub.manager;

import io.cattle.platform.api.pubsub.model.Publish;
import io.cattle.platform.api.pubsub.util.SubscriptionUtils;
import io.cattle.platform.api.pubsub.util.SubscriptionUtils.SubscriptionStyle;
import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class PublishManager extends AbstractNoOpResourceManager {

    private final static Pattern SERVICE_PATTERN = Pattern.compile("^service\\.[a-z.]+$");

    private EventService eventService;

    public PublishManager(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        Publish publish = request.proxyRequestObject(Publish.class);

        Event event = createEvent(publish);

        if (isServiceEvent(event)) {
            eventService.publish(EventVO.newEvent(FrameworkEvents.SERVICE_EVENT)
                    .withData(event)
                    .withResourceId(Long.toString(ApiUtils.getPolicy().getAccountId())));
            return publish;
        }

        if (SubscriptionUtils.getSubscriptionStyle(ApiUtils.getPolicy()) != SubscriptionStyle.RAW && !event.getName().startsWith(Event.REPLY_PREFIX)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }

        eventService.publish(event);

        return publish;
    }

    private boolean isServiceEvent(Event event) {
        String eventName = event.getName();
        return eventName != null && SERVICE_PATTERN.matcher(eventName).matches();
    }

    private Event createEvent(Publish publish) {
        EventVO<Object> event = new EventVO<>();

        if (publish.getId() != null) {
            event.setId(publish.getId());
        }

        event.setId(publish.getId());
        event.setName(publish.getName());
        event.setResourceId(getResourceId(publish.getResourceId()));
        event.setResourceType(publish.getResourceType());
        event.setData(publish.getData());
        event.setTransitioning(publish.getTransitioning());
        event.setTransitioningMessage(publish.getTransitioningMessage());
        event.setTransitioningProgress(publish.getTransitioningProgress());

        if (publish.getTime() != null) {
            event.setTime(new Date(publish.getTime()));
        }

        List<String> previous = publish.getPreviousIds();
        if (previous != null) {
            event.setPreviousIds(previous.toArray(new String[previous.size()]));
        }

        return event;
    }

    protected String getResourceId(String resourceId) {
        IdFormatter formatter = ApiContext.getContext().getIdFormatter();
        return formatter.parseId(resourceId);
    }

}