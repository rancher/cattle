package io.cattle.platform.agent.impl;

import io.cattle.platform.agent.AgentRequest;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.exception.AgentRemovedException;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.impl.AbstractEventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.util.concurrent.ListenableFuture;

public class RemoteAgentImpl implements RemoteAgent {

    private static final Set<String> FRIENDLY_REPLY = new HashSet<>(Arrays.asList("compute.instance.activate"));

    JsonMapper jsonMapper;
    ObjectManager objectManager;
    EventService rawEventService;
    EventService wrappedEventService;
    Long agentId;

    public RemoteAgentImpl(JsonMapper jsonMapper, ObjectManager objectManager, EventService rawEventService, EventService wrappedEventService, Long agentId) {
        this.jsonMapper = jsonMapper;
        this.objectManager = objectManager;
        this.rawEventService = rawEventService;
        this.wrappedEventService = wrappedEventService;
        this.agentId = agentId;
    }

    @Override
    public long getAgentId() {
        return agentId;
    }

    protected AgentRequest createRequest(Event event) {
        return new AgentRequest(agentId, event);
    }

    @Override
    public void publish(Event event) {
        wrappedEventService.publish(createRequest(event));
    }

    @Override
    public <T extends Event> T callSync(Event event, Class<T> reply, long timeout) {
        return callSync(event, reply, new EventCallOptions(AbstractEventService.DEFAULT_RETRIES.get(), timeout));
    }

    @Override
    public <T extends Event> T callSync(Event event, Class<T> reply, EventCallOptions options) {
        /*
         * NOTE: Forever blocking get() used only because underlying future will
         * always timeout
         */
        try {
            return AsyncUtils.get(call(event, reply, options));
        } catch (TimeoutException e) {
            Agent agent = objectManager.loadResource(Agent.class, agentId);
            if (agent == null || agent.getRemoved() != null) {
                throw new AgentRemovedException("Agent [" + agentId + "] is removed", event);
            }
            throw e;
        } catch (AgentRemovedException e) {
            throw e;
        } catch (EventExecutionException e) {
            /*
             * This is done so that the exception will have a better stack
             * trace. Normally the exceptions from a future will have a pretty
             * sparse stack not giving too much context
             */
            throw EventExecutionException.fromEvent(e.getEvent());
        }
    }

    @Override
    public <T extends Event> ListenableFuture<T> call(final Event event, final Class<T> reply, long timeout) {
        return call(event, reply, new EventCallOptions(AbstractEventService.DEFAULT_RETRIES.get(), timeout));
    }

    @Override
    public <T extends Event> ListenableFuture<T> call(final Event event, final Class<T> reply, EventCallOptions options) {
        AgentRequest request = createRequest(event);
        return EventCallProgressHelper.call(wrappedEventService, request, reply, options, new EventResponseMarshaller() {
            @Override
            public <V> V convert(Event resultEvent, Class<V> reply) {
                return getReply(event, resultEvent, reply);
            }
        });
    }

    protected <T> T getReply(Event inputEvent, Event resultEvent, Class<T> reply) {
        if (resultEvent.getData() == null) {
            return null;
        }

        T commandReply = jsonMapper.convertValue(resultEvent.getData(), reply);
        if (FRIENDLY_REPLY.contains(inputEvent.getName())) {
            EventVO<?> publishEvent = null;
            if (commandReply instanceof EventVO) {
                publishEvent = (EventVO<?>) commandReply;
            } else {
                publishEvent = jsonMapper.convertValue(resultEvent.getData(), EventVO.class);
            }

            publishEvent.setName(inputEvent.getName() + Event.REPLY_SUFFIX);
            rawEventService.publish(publishEvent);
        }

        return commandReply;
    }

    @Override
    public Event callSync(Event event) {
        return callSync(event, AbstractEventService.DEFAULT_TIMEOUT.get());
    }

    @Override
    public Event callSync(Event event, EventCallOptions options) {
        return callSync(event, EventVO.class, options);
    }

    @Override
    public Event callSync(Event event, long timeout) {
        return callSync(event, EventVO.class, timeout);
    }

    @Override
    public ListenableFuture<? extends Event> call(Event event) {
        return call(event, AbstractEventService.DEFAULT_TIMEOUT.get());
    }

    @Override
    public ListenableFuture<? extends Event> call(Event event, EventCallOptions options) {
        return call(event, EventVO.class, options);
    }

    @Override
    public ListenableFuture<? extends Event> call(Event event, long timeout) {
        return call(event, EventVO.class, timeout);
    }

    @Override
    public <T extends Event> T callSync(Event event, Class<T> reply) {
        return callSync(event, reply, AbstractEventService.DEFAULT_TIMEOUT.get());
    }

    @Override
    public <T extends Event> ListenableFuture<T> call(Event event, Class<T> reply) {
        return call(event, reply, AbstractEventService.DEFAULT_TIMEOUT.get());
    }

}
