package io.cattle.platform.agent.impl;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.LinkedHashMap;
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
    @Inject
    ResourceMonitor resourceMonitor;

    LoadingCache<Long, RemoteAgent> cache = CacheBuilder.newBuilder().expireAfterWrite(15L, TimeUnit.MINUTES).build(new CacheLoader<Long, RemoteAgent>() {
        @Override
        public RemoteAgent load(Long agentId) throws Exception {
            EventService wrappedEventService = getWrappedEventService(agentId);
            if (wrappedEventService == null) {
                wrappedEventService = buildDelegate(agentId);
            }

            if (wrappedEventService == null) {
                wrappedEventService = eventService;
            }

            return new RemoteAgentImpl(jsonMapper, objectManager, eventService, wrappedEventService, agentId);
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

        return new WrappedEventService(agentId, false, eventService, null, jsonMapper, delegateDao);
    }

    protected EventService buildDelegate(long agentId) {
        Map<String, Object> instanceData = new LinkedHashMap<>();
        Agent hostAgent = getAgentForDelegate(agentId, instanceData);
        if (hostAgent == null) {
            return null;
        }

        String hostAgentUri = hostAgent.getUri();
        if (hostAgentUri != null && !hostAgentUri.startsWith(EVENTING)) {
            return null;
        }

        return new WrappedEventService(hostAgent.getId(), true, eventService, instanceData, jsonMapper, delegateDao);
    }

    @Override
    public Agent getAgentForDelegate(long agentId, Map<String, Object> outInstanceData) {
        final Agent agent = objectManager.loadResource(Agent.class, agentId);
        if (agent == null) {
            return null;
        }

        String uri = agent.getUri();
        if (uri != null && !uri.startsWith(DELEGATE)) {
            return null;
        }

        Instance instance = delegateDao.getInstance(agent);
        if (instance == null) {
            log.error("Failed to find instance to delegate to for agent [{}] uri [{}]", agent.getId(), agent.getUri());
            throw new IllegalStateException("Delegate [" + agent.getUri() + "] has no instance associated");
        }

        instance = resourceMonitor.waitFor(instance, new ResourcePredicate<Instance>() {
            @Override
            public boolean evaluate(Instance obj) {
                return delegateDao.getHost(agent) != null;
            }

            @Override
            public String getMessage() {
                return "agent wait for allocated";
            }
        });

        Host host = delegateDao.getHost(agent);
        if (host == null || host.getAgentId() == null) {
            log.error("Failed to find host to delegate to for agent [{}] uri [{}]", agent.getId(), agent.getUri());
            return null;
        }

        if (outInstanceData != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> instanceData = jsonMapper.convertValue(instance, Map.class);
            outInstanceData.putAll(instanceData);
        }
        return objectManager.loadResource(Agent.class, host.getAgentId());
    }

    public static Long getAgentId(Object resource) {
        Object obj = ObjectUtils.getPropertyIgnoreErrors(resource, "agentId");
        if (obj instanceof Long) {
            return (Long) obj;
        }

        return null;
    }

}
