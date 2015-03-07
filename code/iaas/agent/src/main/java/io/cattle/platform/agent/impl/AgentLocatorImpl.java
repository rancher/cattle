package io.cattle.platform.agent.impl;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;

public class AgentLocatorImpl implements AgentLocator {

    private static final DynamicLongProperty CACHE_TIME = ArchaiusUtil.getLong("agent.group.id.cache.seconds");
    private static final DynamicBooleanProperty DIRECT = ArchaiusUtil.getBoolean("agent.direct.request");

    ObjectManager objectManager;
    EventService eventService;
    JsonMapper jsonMapper;
    Cache<Long, Long> groupIdCache = CacheBuilder.newBuilder().expireAfterWrite(CACHE_TIME.get(), TimeUnit.SECONDS).build();

    @Override
    public RemoteAgent lookupAgent(Object resource) {
        if (resource == null) {
            return null;
        }

        Long agentId = null;
        Long groupId = null;

        if (resource instanceof Long) {
            agentId = (Long) resource;
        } else if (resource instanceof Agent) {
            agentId = ((Agent) resource).getId();
            groupId = ((Agent) resource).getAgentGroupId();
        }

        if (agentId == null) {
            agentId = getAgentId(resource);
        }

        if (agentId == null) {
            return null;
        }

        if (DIRECT.get() && groupId == null) {
            Agent agent = objectManager.loadResource(Agent.class, agentId);
            groupId = agent.getAgentGroupId();
        }

        return agentId == null ? null : new RemoteAgentImpl(jsonMapper, eventService, agentId, groupId);
    }

    protected Long getAgentId(Object resource) {
        Object obj = ObjectUtils.getPropertyIgnoreErrors(resource, "agentId");
        if (obj instanceof Long) {
            return (Long) obj;
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

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

}