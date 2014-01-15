package io.github.ibuildthecloud.agent.server.connection.impl;

import io.github.ibuildthecloud.agent.server.connection.AgentConnection;
import io.github.ibuildthecloud.agent.server.connection.AgentConnectionFactory;
import io.github.ibuildthecloud.agent.server.connection.AgentConnectionManager;
import io.github.ibuildthecloud.agent.server.lock.AgentConnectionLockDefinition;
import io.github.ibuildthecloud.agent.server.lock.AgentConnectionManagementLock;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.core.model.AgentGroup;
import io.github.ibuildthecloud.dstack.lock.LockCallback;
import io.github.ibuildthecloud.dstack.lock.LockDelegator;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;

public class AgentConnectionManagerImpl implements AgentConnectionManager {

    private static final Logger log =LoggerFactory.getLogger(AgentConnectionManagerImpl.class);

    public static final String WILDCARD = "*";
    public static final String UNASSIGNED = "!";

    private static final DynamicStringProperty AGENT_GROUPS = ArchaiusUtil.getString("agent.groups");
    private static final DynamicLongProperty AGENT_LOCK_FAILURE_CACHE_TIME = ArchaiusUtil.getLong("agent.lock.failure.cache.time.millis");

    ObjectManager objectManager;
    Set<String> groups;
    LockDelegator lockDelegator;
    LockManager lockManager;
    Cache<String, Object> cache = CacheBuilder.newBuilder()
                                    .expireAfterWrite(AGENT_LOCK_FAILURE_CACHE_TIME.get(), TimeUnit.MILLISECONDS)
                                    .build();
    Map<Long,AgentConnection> connections = new ConcurrentHashMap<Long,AgentConnection>();
    List<AgentConnectionFactory> factories;

    @Override
    public AgentConnection getConnection(Agent agent) {
        if ( agent == null )
            return null;

        if ( ! shouldHandleAgent(agent) ) {
            closeIfExists(agent);
            return null;
        }

        LockDefinition lockDefinition = getLockDefinition(agent);
        if ( ! haveLock(lockDefinition) ) {
            closeIfExists(agent);
            return null;
        }

        AgentConnection connection = connections.get(agent.getId());
        if ( connection != null ) {
            if ( ! connection.isOpen() || ! ObjectUtils.equals(agent.getUri(), connection.getUri()) ) {
                closeConnection(agent, connection);
                return getConnection(agent);
            }
            return connection;
        }

        return createConnection(agent);
    }

    protected void closeIfExists(Agent agent) {
        AgentConnection connection = connections.get(agent.getId());
        if ( connection != null ) {
            closeConnection(agent, connection);
        }
    }

    protected synchronized void closeConnection(Agent agent, AgentConnection connection) {
        if ( connection == null ) {
            return;
        }

        connection.close();
        connections.remove(agent.getId());

        LockDefinition lockDef = getLockDefinition(agent);
        lockDelegator.unlock(lockDef);
        cache.invalidate(agent.getId());
    }

    protected AgentConnection createConnection(final Agent agent) {
        return lockManager.lock(new AgentConnectionManagementLock(agent), new LockCallback<AgentConnection>() {
            @Override
            public AgentConnection doWithLock() {
                return createConnectionWithLock(agent);
            }
        });
    }

    protected AgentConnection createConnectionWithLock(Agent agent) {
        log.info("Creating connection to agent [{}] [{}]", agent.getId(), agent.getUri());

        AgentConnection connection = connections.get(agent.getId());
        if ( connection != null ) {
            return connection;
        }

        for ( AgentConnectionFactory factory : factories ) {
            try {
                connection = factory.createConnection(agent);
            } catch (IOException e) {
                log.error("Failed to create connection for agent [{}]", agent.getId(), e);
                return null;
            }

            if ( connection != null ) {
                break;
            }
        }

        if ( connection == null ) {
            log.error("No connection factory created a connection for agent [{}] [{}]", agent.getId(), agent.getUri());
        } else {
            connections.put(agent.getId(), connection);
        }

        return connection;
    }

    protected boolean shouldHandleAgent(Agent agent) {
        AgentGroup group = objectManager.loadResource(AgentGroup.class, agent.getAgentGroupId());
        if ( group == null ) {
            return groups.contains(WILDCARD) || groups.contains(UNASSIGNED);
        }

        return groups.contains(group.getId()) || groups.contains(WILDCARD);
    }

    protected boolean haveLock(LockDefinition lockDef) {
        Object dontCheck = cache.getIfPresent(lockDef.getLockId());

        if ( dontCheck != null ) {
            return false;
        }

        boolean success = lockDelegator.tryLock(lockDef);
        if ( ! success ) {
            cache.put(lockDef.getLockId(), new Object());
        }

        return success;
    }

    @PostConstruct
    public void init() {
        groups = new HashSet<String>();
        for ( String group : AGENT_GROUPS.get().trim().split("\\s*,\\s*") ) {
            groups.add(group);
        }
    }

    protected LockDefinition getLockDefinition(Agent agent) {
        return new AgentConnectionLockDefinition(agent);
    }

    public LockDelegator getLockDelegator() {
        return lockDelegator;
    }

    @Inject
    public void setLockDelegator(LockDelegator lockDelegator) {
        this.lockDelegator = lockDelegator;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public List<AgentConnectionFactory> getFactories() {
        return factories;
    }

    @Inject
    public void setFactories(List<AgentConnectionFactory> factories) {
        this.factories = factories;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }
}
