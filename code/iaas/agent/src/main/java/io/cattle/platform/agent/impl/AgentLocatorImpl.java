package io.cattle.platform.agent.impl;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class AgentLocatorImpl implements AgentLocator {

    private static final String EVENTING = "event://";

    @Inject
    AgentDao agentDao;
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

        return new WrappedEventService(agentId, eventService, jsonMapper, agentDao);
    }

    public static Long getAgentId(Object resource) {
        Object obj = ObjectUtils.getPropertyIgnoreErrors(resource, "agentId");
        if (obj instanceof Long) {
            return (Long) obj;
        }

        return null;
    }

}
