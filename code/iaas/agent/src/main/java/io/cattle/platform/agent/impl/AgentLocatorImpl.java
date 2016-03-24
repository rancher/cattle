package io.cattle.platform.agent.impl;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class AgentLocatorImpl implements AgentLocator {

    private static final String EVENTING = "event://";
    private static final String DELEGATE = "delegate://";

    private static final Logger log = LoggerFactory.getLogger(AgentLocatorImpl.class);

    @Inject
    AgentDao delegateDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    EventService eventService;
    @Inject
    JsonMapper jsonMapper;

    LoadingCache<Long, RemoteAgent> cache = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build(new CacheLoader<Long, RemoteAgent>() {
        @Override
        public RemoteAgent load(Long agentId) throws Exception {
            EventService wrappedEventService = getWrappedEventService(agentId);
            if (wrappedEventService == null) {
                wrappedEventService = buildDelegate(agentId);
            }

            if (wrappedEventService == null) {
                wrappedEventService = eventService;
            }

            return new RemoteAgentImpl(jsonMapper, eventService, wrappedEventService, agentId);
        }
    });

    @Override
    public RemoteAgent lookupAgent(Object resource) {
        if (resource == null) {
            return null;
        }

        Long agentId = null;

        if (resource instanceof Long) {
            agentId = (Long) resource;
        } else if (resource instanceof Agent) {
            agentId = ((Agent) resource).getId();
        }

        if (agentId == null) {
            agentId = getAgentId(resource);
        }

        if (agentId == null) {
            return null;
        }

        return cache.getUnchecked(agentId);
    }

    protected EventService getWrappedEventService(long agentId) {
        Agent agent = objectManager.loadResource(Agent.class, agentId);
        if (agent == null) {
            return null;
        }

        String uri = agent.getUri();
        if (uri != null && !uri.startsWith(EVENTING)) {
            return null;
        }

        return new WrappedEventService(agentId, false, eventService, null, jsonMapper);
    }

    protected EventService buildDelegate(long agentId) {
        Agent agent = objectManager.loadResource(Agent.class, agentId);
        if (agent == null) {
            return null;
        }

        String uri = agent.getUri();
        if (uri != null && !uri.startsWith(DELEGATE)) {
            return null;
        }

        Instance instance = delegateDao.getInstance(agent);

        if (instance == null) {
            log.info("Failed to find instance to delegate to for agent [{}] uri [{}]", agent.getId(), agent.getUri());
            return null;
        }

        if (!InstanceConstants.STATE_RUNNING.equals(instance.getState())) {
            log.info("Instance [{}] is not running, actual state [{}]", instance.getId(), instance.getState());
            return null;
        }

        Host host = delegateDao.getHost(agent);

        if (host == null || host.getAgentId() == null) {
            log.error("Failed to find host to delegate to for agent [{}] uri [{}]", agent.getId(), agent.getUri());
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> instanceData = jsonMapper.convertValue(instance, Map.class);

        return new WrappedEventService(host.getAgentId(), true, eventService, instanceData, jsonMapper);
    }

    public static Long getAgentId(Object resource) {
        Object obj = ObjectUtils.getPropertyIgnoreErrors(resource, "agentId");
        if (obj instanceof Long) {
            return (Long) obj;
        }

        return null;
    }

}