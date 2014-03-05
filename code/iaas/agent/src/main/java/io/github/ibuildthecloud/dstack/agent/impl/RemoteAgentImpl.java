package io.github.ibuildthecloud.dstack.agent.impl;

import io.github.ibuildthecloud.dstack.agent.AgentRequest;
import io.github.ibuildthecloud.dstack.agent.RemoteAgent;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.async.utils.AsyncUtils;
import io.github.ibuildthecloud.dstack.eventing.EventCallOptions;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.exception.EventExecutionException;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.json.JsonMapper;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;

public class RemoteAgentImpl implements RemoteAgent {

    private static final DynamicLongProperty AGENT_DEFAULT_TIMEOUT = ArchaiusUtil.getLong("agent.timeout.millis");
    private static final DynamicIntProperty AGENT_RETRIES = ArchaiusUtil.getInt("agent.retries");

    JsonMapper jsonMapper;
    EventService eventService;
    Long agentId;
    Long groupId;

    public RemoteAgentImpl(JsonMapper jsonMapper, EventService eventService,
            Long agentId, Long groupId) {
        this.jsonMapper = jsonMapper;
        this.eventService = eventService;
        this.agentId = agentId;
        this.groupId = groupId;
    }

    protected Event createRequest(Event event) {
        return new AgentRequest(agentId, groupId, event);
    }

    @Override
    public void publish(Event event) {
        eventService.publish(createRequest(event));
    }

    @Override
    public <T extends Event> T callSync(Event event, Class<T> reply, long timeout) {
        return callSync(event, reply, new EventCallOptions(AGENT_RETRIES.get(), timeout));
    }

    @Override
    public <T extends Event> T callSync(Event event, Class<T> reply, EventCallOptions options) {
        /* NOTE: Forever blocking get() used only because underlying future will always timeout */
        try {
            return AsyncUtils.get(call(event, reply, options));
        } catch ( EventExecutionException e ) {
            /* This is done so that the exception will have a better stack trace.
             * Normally the exceptions from a future will have a pretty sparse stack
             * not giving too much context
             */
            throw new EventExecutionException(e);
        }
    }

    @Override
    public <T extends Event> ListenableFuture<T> call(final Event event, final Class<T> reply, long timeout) {
        return call(event, reply, new EventCallOptions(AGENT_RETRIES.get(), timeout));
    }

    @Override
    public <T extends Event> ListenableFuture<T> call(final Event event, final Class<T> reply, EventCallOptions options) {
        Event request = createRequest(event);

        ListenableFuture<Event> future = eventService.call(request, options);
        return Futures.transform(future, new Function<Event, T>() {
            @Override
            public T apply(Event input) {
                if ( input.getData() == null ) {
                    return null;
                }

                T commandReply = jsonMapper.convertValue(input.getData(), reply);
                EventVO<?> publishEvent = null;
                if ( commandReply instanceof EventVO ) {
                    publishEvent = (EventVO<?>)commandReply;
                } else {
                    publishEvent = jsonMapper.convertValue(input.getData(), EventVO.class);
                }

                publishEvent.setName(event.getName() + Event.REPLY_SUFFIX);
                eventService.publish(publishEvent);

                return commandReply;
            }
        });
    }

    @Override
    public Event callSync(Event event) {
        return callSync(event, AGENT_DEFAULT_TIMEOUT.get());
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
        return call(event, AGENT_DEFAULT_TIMEOUT.get());
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
        return callSync(event, reply, AGENT_DEFAULT_TIMEOUT.get());
    }

    @Override
    public <T extends Event> ListenableFuture<T> call(Event event, Class<T> reply) {
        return call(event, reply, AGENT_DEFAULT_TIMEOUT.get());
    }

}
