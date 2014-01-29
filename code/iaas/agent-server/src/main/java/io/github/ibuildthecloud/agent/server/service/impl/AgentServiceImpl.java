package io.github.ibuildthecloud.agent.server.service.impl;

import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.agent.server.connection.AgentConnectionManager;
import io.github.ibuildthecloud.agent.server.service.AgentService;
import io.github.ibuildthecloud.dstack.async.retry.RetryTimeoutService;
import io.github.ibuildthecloud.dstack.async.utils.AsyncUtils;
import io.github.ibuildthecloud.dstack.async.utils.TimeoutException;
import io.github.ibuildthecloud.dstack.core.constants.CommonStatesConstants;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.exception.EventExecutionException;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private static final Set<String> GOOD_AGENT_STATES = new HashSet<String>(Arrays.asList(
            CommonStatesConstants.ACTIVATING,
            CommonStatesConstants.ACTIVE
        ));

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

        final Event agentEvent = getAgentEvent(event);
        if ( agentEvent == null ) {
            return;
        }

        AgentConnection connection = connectionManager.getConnection(agent);
        if ( connection != null ) {
            eventService.publish(agentEvent);
            final ListenableFuture<Event> future = connection.execute(agentEvent);
            future.addListener(new NoExceptionRunnable() {
                @Override
                protected void doRun() throws Exception {
                    try {
                        handleResponse(event, AsyncUtils.get(future));
                    } catch ( EventExecutionException e ) {
                        handleError(event, e.getEvent());
                    } catch ( TimeoutException t ) {
                        log.info("Timeout waiting for response to [{}] id [{}]", agentEvent.getName(), agentEvent.getId());
                    }
                }
            }, executorService);
        }
    }

    protected void handleResponse(Event request, Event agentResponse) {
        if ( request.getReplyTo() != null ) {
            EventVO<Object> response = EventVO.reply(request);
            response.setData(agentResponse);
            eventService.publish(response);
        }
    }

    protected void handleError(Event request, Event agentResponse) {
        EventVO<Object> response = EventVO.reply(request);
        response.setData(agentResponse);
        response.setTransitioning(agentResponse.getTransitioning());
        response.setTransitioningInternalMessage(agentResponse.getTransitioningInternalMessage());
        response.setTransitioningMessage(agentResponse.getTransitioningMessage());
        response.setTransitioningProgress(agentResponse.getTransitioningProgress());

        eventService.publish(response);
    }

    protected Event getAgentEvent(Event event) {
        if ( event.getData() == null ) {
            return null;
        }

        EventVO<?> agentEvent = jsonMapper.convertValue(event.getData(), EventVO.class);
        agentEvent.setReplyTo(agentEvent.getName() + Event.REPLY_SUFFIX);

        return agentEvent;
    }

    protected Agent getAgent(Event event) {
        Agent agent = objectManager.loadResource(Agent.class, event.getResourceId());
        if ( agent != null && GOOD_AGENT_STATES.contains(agent.getState()) ) {
            return agent;
        }

        return null;
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
