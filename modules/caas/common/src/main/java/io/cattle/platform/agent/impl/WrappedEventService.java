package io.cattle.platform.agent.impl;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventListener;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.exception.AgentRemovedException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.json.JsonMapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WrappedEventService implements EventService {

    public static final Set<String> GOOD_AGENT_STATES = new HashSet<>(Arrays.asList(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE,
            AgentConstants.STATE_RECONNECTING));

    long agentId;
    EventService eventService;
    JsonMapper jsonMapper;
    AgentDao agentDao;

    public WrappedEventService(long agentId, EventService eventService, JsonMapper jsonMapper, AgentDao agentDao) {
        super();
        this.agentId = agentId;
        this.eventService = eventService;
        this.jsonMapper = jsonMapper;
        this.agentDao = agentDao;
    }

    protected Event buildEvent(Event request) {
        Event event = null;
        Object payload = request.getData();
        if (payload instanceof Event) {
            event = new EventVO<>((Event)payload).withReplyTo(((Event)payload).getName() + Event.REPLY_SUFFIX);
        } else {
            throw new IllegalArgumentException("Event does not have embedded event");
        }
        return new EventVO<>(event).withName(FrameworkEvents.appendAgent(event.getName(), agentId));
    }

    @Override
    public boolean publish(Event request) {
        return eventService.publish(buildEvent(request));
    }

    @Override
    public ListenableFuture<Event> call(final Event request, EventCallOptions callOptions) {
        Event unwrappedEvent = buildEvent(request);
        String state = agentDao.getAgentState(agentId);
        if (state == null) {
            return AsyncUtils.error(new AgentRemovedException("Agent [" + agentId + "] is removed", request));
        } else if (!GOOD_AGENT_STATES.contains(state)) {
            return AsyncUtils.error(new TimeoutException());
        }

        return EventCallProgressHelper.call(eventService, unwrappedEvent, Event.class, callOptions, new EventResponseMarshaller() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> T convert(Event resultEvent, Class<T> reply) {
                return (T) new EventVO<>().withName(request.getReplyTo()).withData(resultEvent);
            }
        });
    }

    /* Boilerplate to implement interface */
    @Override
    public ListenableFuture<?> subscribe(String eventName, EventListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unsubscribe(String eventName, EventListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unsubscribe(EventListener listener) {
        throw new UnsupportedOperationException();
    }

}