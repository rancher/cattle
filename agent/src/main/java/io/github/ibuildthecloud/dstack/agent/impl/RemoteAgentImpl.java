package io.github.ibuildthecloud.dstack.agent.impl;

import io.github.ibuildthecloud.dstack.agent.AgentRequest;
import io.github.ibuildthecloud.dstack.agent.RemoteAgent;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.async.utils.AsyncUtils;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.json.JsonMapper;

import java.util.concurrent.ExecutorService;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;

public class RemoteAgentImpl implements RemoteAgent {

    private static final DynamicLongProperty AGENT_DEFAULT_TIMEOUT = ArchaiusUtil.getLong("agent.timeout.millis");
    private static final DynamicIntProperty AGENT_RETRIES = ArchaiusUtil.getInt("agent.retries");

    ExecutorService executorService;
    JsonMapper jsonMapper;
    EventService eventService;
    Long agentId;

    public RemoteAgentImpl(ExecutorService executorService, JsonMapper jsonMapper, EventService eventService,
            Long agentId) {
        this.executorService = executorService;
        this.jsonMapper = jsonMapper;
        this.eventService = eventService;
        this.agentId = agentId;
    }

    protected Event createRequest(Event event) {
        return new AgentRequest(agentId, event);
    }

    @Override
    public void publish(Event event) {
        eventService.publish(createRequest(event));
    }

    @Override
    public <T extends Event> T callSync(Event event, Class<T> reply, long timeout) {
        /* NOTE: Forever blocking get() used only because underlying future will always timeout */
        return AsyncUtils.get(call(event, reply, timeout));
    }

    @Override
    public <T extends Event> ListenableFuture<T> call(Event event, final Class<T> reply, long timeout) {
        Event request = createRequest(event);

        ListenableFuture<Event> future = eventService.call(request, AGENT_RETRIES.get(), timeout);
        return Futures.transform(future, new Function<Event, T>() {
            @Override
            public T apply(Event input) {
                return jsonMapper.convertValue(input, reply);
            }
        });
    }

    @Override
    public Event callSync(Event event) {
        return callSync(event, AGENT_DEFAULT_TIMEOUT.get());
    }

    @Override
    public Event callSync(Event event, long timeout) {
        return callSync(event, Event.class, timeout);
    }

    @Override
    public ListenableFuture<Event> call(Event event) {
        return call(event, AGENT_DEFAULT_TIMEOUT.get());
    }

    @Override
    public ListenableFuture<Event> call(Event event, long timeout) {
        return call(event, Event.class, timeout);
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
