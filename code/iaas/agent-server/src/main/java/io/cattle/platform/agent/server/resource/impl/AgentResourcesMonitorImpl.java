package io.cattle.platform.agent.server.resource.impl;

import io.cattle.platform.agent.server.resource.AgentResourceChangeHandler;
import io.cattle.platform.agent.server.resource.AgentResourcesEventListener;
import io.cattle.platform.agent.server.util.AgentConnectionUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.lock.LockCallbackWithException;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.InitializationTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netflix.config.DynamicLongProperty;

public class AgentResourcesMonitorImpl implements AgentResourcesEventListener, InitializationTask {

    private static final Logger log = LoggerFactory.getLogger(AgentResourcesMonitorImpl.class);
    private static final DynamicLongProperty CACHE_RESOURCE = ArchaiusUtil.getLong("agent.resource.monitor.cache.resource.seconds");

    LockDelegator lockDelegator;
    LockManager lockManager;
    Map<String,AgentResourceChangeHandler> handlers;
    List<AgentResourceChangeHandler> changeHandlers;
    Cache<ImmutablePair<String, String>, Map<String,Object>> resourceCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_RESOURCE.get(), TimeUnit.SECONDS)
            .build();

    @Override
    public void pingReply(Ping ping) {
        String agentIdStr = ping.getResourceId();
        if ( agentIdStr == null ) {
            return;
        }

        long agentId = Long.parseLong(agentIdStr);
        LockDefinition lockDef = AgentConnectionUtils.getConnectionLock(agentId);
        if ( ! lockDelegator.isLocked(lockDef) ) {
            return;
        }

        if ( ping.getData() == null ) {
            return;
        }

        Queue<Map<String,Object>> queue = new LinkedBlockingQueue<Map<String,Object>>(ping.getData().getResources());
        long max = queue.size() * 3;
        long count = 0;
        while ( queue.size() > 0 ) {
            Map<String,Object> agentResource = queue.remove();
            try {
                processResource(agentId, ping, agentResource);
            } catch ( MissingDependencyException e ) {
                queue.add(agentResource);
            }

            if ( count++ > max && queue.size() > 0 ) {
                throw new IllegalStateException("For agent [" + agentIdStr +
                        "] failed to add resources, dependency may not be found, resource=" + queue);
            }
        }
    }

    protected void processResource(final long agentId, Ping ping, final Map<String,Object> agentResource) throws MissingDependencyException {
        final String type = ObjectUtils.toString(agentResource.get(ObjectMetaDataManager.TYPE_FIELD), null);
        final String uuid = ObjectUtils.toString(agentResource.get(ObjectMetaDataManager.UUID_FIELD), null);

        if ( type == null || uuid == null ) {
            log.error("type [{}] or uuid [{}] is null for resource on pong from agent [{}]", type, uuid, ping.getResourceId());
            return;
        }

        final AgentResourceChangeHandler handler = handlers.get(type);
        if ( handler == null ) {
            log.debug("Unknown resource type [{}] for pong", type);
            return;
        }

        ImmutablePair<String, String> key = new ImmutablePair<String, String>(type, uuid);
        Map<String,Object> cachedResource = resourceCache.getIfPresent(key);
        if ( cachedResource == null ) {
            cachedResource = handler.load(uuid);
            if ( cachedResource != null ) {
                resourceCache.put(key, cachedResource);
            }
        }

        if ( cachedResource == null ) {
            cachedResource = lockManager.lock(new AgentResourceCreateLock(uuid),
                    new LockCallbackWithException<Map<String,Object>,MissingDependencyException>() {
                @Override
                public Map<String, Object> doWithLock() throws MissingDependencyException {
                    Map<String,Object> cachedResource = handler.load(uuid);
                    if ( cachedResource != null ) {
                        return cachedResource;
                    }

                    handler.newResource(agentId, agentResource);
                    return null;
                }
            }, MissingDependencyException.class);
        }

        if ( cachedResource != null && handler.areDifferent(agentResource, cachedResource) ) {
            handler.changed(agentResource, cachedResource);
        }
    }

    public LockDelegator getLockDelegator() {
        return lockDelegator;
    }

    @Inject
    public void setLockDelegator(LockDelegator lockDelegator) {
        this.lockDelegator = lockDelegator;
    }

    public List<AgentResourceChangeHandler> getChangeHandlers() {
        return changeHandlers;
    }

    @Inject
    public void setChangeHandlers(List<AgentResourceChangeHandler> changeHandlers) {
        this.changeHandlers = changeHandlers;
    }

    @Override
    public void start() {
        handlers = new HashMap<String, AgentResourceChangeHandler>();
        for ( AgentResourceChangeHandler handler : changeHandlers ) {
            String type = handler.getType();
            if ( ! handlers.containsKey(type) ) {
                handlers.put(type, handler);
            }
        }
    }

    @Override
    public void stop() {
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

}
