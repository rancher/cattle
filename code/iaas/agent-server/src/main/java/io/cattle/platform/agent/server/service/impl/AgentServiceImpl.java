package io.cattle.platform.agent.server.service.impl;

import io.cattle.platform.agent.server.connection.AgentConnection;
import io.cattle.platform.agent.server.connection.AgentConnectionManager;
import io.cattle.platform.agent.server.service.AgentService;
import io.cattle.platform.async.retry.RetryTimeoutService;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventProgress;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.eventing.util.EventUtils;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    public static final Set<String> GOOD_AGENT_STATES = CollectionUtils.set(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE,
            AgentConstants.STATE_RECONNECTING);

    AgentConnectionManager connectionManager;
    ObjectManager objectManager;
    JsonMapper jsonMapper;
    RetryTimeoutService timeoutService;
    ExecutorService executorService;
    EventService eventService;

    @Override
    public void execute(final Event event) {
        Agent agent = getAgent(event);
        if (agent == null) {
            return;
        }

        final Event agentEvent = getAgentEvent(event);
        if (agentEvent == null) {
            return;
        }

        if (IaasEvents.AGENT_CLOSE.equals(agentEvent.getName())) {
            connectionManager.closeConnection(agent);
            handleResponse(event, EventVO.reply(agentEvent));
        } else {
            if (!GOOD_AGENT_STATES.contains(agent.getState())) {
                log.info("Dropping event [{}] [{}] for agent [{}] in state [{}]", event.getName(), event.getId(), agent.getId(), agent.getState());
                return;
            }

            AgentConnection connection = connectionManager.getConnection(agent);
            if (connection != null) {
                eventService.publish(agentEvent);
                final ListenableFuture<Event> future = connection.execute(agentEvent, new EventProgress() {
                    @Override
                    public void progress(Event agentResponse) {
                        handleResponse(event, agentResponse);
                    }
                });

                future.addListener(new NoExceptionRunnable() {
                    @Override
                    protected void doRun() throws Exception {
                        try {
                            Event agentEventResponse = AsyncUtils.get(future);
                            if (Event.TRANSITIONING_ERROR.equals(agentEventResponse.getTransitioning())) {
                                throw new EventExecutionException(agentEventResponse);
                            }

                            handleResponse(event, agentEventResponse);
                        } catch (EventExecutionException e) {
                            handleError(event, e.getEvent());
                        } catch (TimeoutException t) {
                            log.info("Timeout waiting for response to [{}] id [{}]", agentEvent.getName(), agentEvent.getId());
                        }
                    }
                }, executorService);
            }
        }
    }

    protected void handleResponse(Event request, Event agentResponse) {
        if (request.getReplyTo() != null) {
            EventVO<Object> response = EventVO.reply(request);
            response.setData(agentResponse);
            EventUtils.copyTransitioning(agentResponse, response);

            eventService.publish(response);
        }
    }

    protected void handleError(Event request, Event agentResponse) {
        EventVO<Object> response = EventVO.reply(request);
        if (response.getName() == null) {
            return;
        }

        response.setData(agentResponse);
        response.setTransitioning(agentResponse.getTransitioning());
        response.setTransitioningInternalMessage(agentResponse.getTransitioningInternalMessage());
        response.setTransitioningMessage(agentResponse.getTransitioningMessage());
        response.setTransitioningProgress(agentResponse.getTransitioningProgress());

        eventService.publish(response);
    }

    protected Event getAgentEvent(Event event) {
        if (event.getData() == null) {
            return null;
        }

        EventVO<?> agentEvent = jsonMapper.convertValue(event.getData(), EventVO.class);
        agentEvent.setReplyTo(agentEvent.getName() + Event.REPLY_SUFFIX);
        if (agentEvent.getTimeoutMillis() == null) {
            agentEvent.setTimeoutMillis(event.getTimeoutMillis());
        }

        return agentEvent;
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
