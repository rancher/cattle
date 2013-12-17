package io.github.ibuildthecloud.agent.server.service.impl;

import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.agent.server.connection.AgentConnectionManager;
import io.github.ibuildthecloud.agent.server.service.AgentService;
import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.async.utils.AsyncUtils;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.eventing.util.EventUtils;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import com.google.common.util.concurrent.ListenableFuture;

public class AgentServiceImpl implements AgentService {

    AgentConnectionManager connectionManager;
    ObjectManager objectManager;
    JsonMapper jsonMapper;
    RetryTimeoutService timeoutService;
    ExecutorService executorService;
    EventService eventService;

    @Override
    public void execute(final Event event) {
        Agent agent = getAgent(event);
        if ( agent == null ) {
            return;
        }

        Event agentEvent = getAgentEvent(event);
        if ( agentEvent == null ) {
            return;
        }

        AgentConnection connection = connectionManager.getConnection(agent);
        if ( connection != null ) {
            final ListenableFuture<Event> future = connection.execute(event);
            future.addListener(new Runnable() {
                @Override
                public void run() {
                    handleResponse(event, AsyncUtils.get(future));
                }
            }, executorService);
        }
    }

    protected void handleResponse(Event request, Event agentResponse) {
        EventVO response = EventUtils.reply(request);
        response.setData(agentResponse);
        eventService.publish(response);
    }

    protected Event getAgentEvent(Event event) {
        if ( event.getData() == null ) {
            return null;
        }

        return jsonMapper.convertValue(event.getData(), EventVO.class);
    }

    protected Agent getAgent(Event event) {
        return objectManager.loadResource(Agent.class, event.getResourceId());
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AgentConnectionManager getConnectionManager() {
        return connectionManager;
    }

    @Inject
    public void setConnectionManager(AgentConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Inject
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

}
