package io.cattle.platform.agent.connection.delegate;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.connection.delegate.dao.AgentDelegateDao;
import io.cattle.platform.agent.server.connection.AgentConnection;
import io.cattle.platform.agent.server.connection.AgentConnectionFactory;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentDelegateConnectionFactory implements AgentConnectionFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentDelegateConnectionFactory.class);
    private static final String PROTOCOL = "delegate://";

    EventService eventService;
    AgentDelegateDao delegateDao;
    AgentLocator agentLocator;
    JsonMapper jsonMapper;

    @Override
    public AgentConnection createConnection(Agent agent) throws IOException {
        String uri = agent.getUri();
        if ( uri == null || ! uri.startsWith(PROTOCOL) ) {
            return null;
        }

        Instance instance = delegateDao.getInstance(agent);

        if ( instance == null ) {
            log.error("Failed to find instance to delegate to for agent [{}] uri [{}]", agent.getId(), agent.getUri());
            return null;
        }

        if ( ! InstanceConstants.STATE_RUNNING.equals(instance.getState()) ) {
            log.info("Instance [{}] is not running, actual state [{}]", instance.getId(), instance.getState());
            return null;
        }

        Host host = delegateDao.getHost(agent);

        if ( host == null ) {
            log.error("Failed to find host to delegate to for agent [{}] uri [{}]", agent.getId(), agent.getUri());
            return null;
        }

        RemoteAgent remoteAgent = agentLocator.lookupAgent(host);

        if ( remoteAgent == null ) {
            log.error("Failed to find remote agent to delegate to for agent [{}] uri [{}]", agent.getId(), agent.getUri());
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String,Object> instanceData = jsonMapper.convertValue(instance, Map.class);

        return new AgentDelegateConnection(remoteAgent, agent.getId(), agent.getUri(), instanceData, jsonMapper);
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public AgentDelegateDao getDelegateDao() {
        return delegateDao;
    }

    @Inject
    public void setDelegateDao(AgentDelegateDao delegateDao) {
        this.delegateDao = delegateDao;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AgentLocator getAgentLocator() {
        return agentLocator;
    }

    @Inject
    public void setAgentLocator(AgentLocator agentLocator) {
        this.agentLocator = agentLocator;
    }

}
