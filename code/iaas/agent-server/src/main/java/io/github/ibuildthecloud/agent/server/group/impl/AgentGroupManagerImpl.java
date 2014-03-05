package io.github.ibuildthecloud.agent.server.group.impl;

import io.github.ibuildthecloud.agent.server.group.AgentGroupManager;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.core.model.AgentGroup;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.netflix.config.DynamicStringProperty;

public class AgentGroupManagerImpl implements AgentGroupManager {

    private static final DynamicStringProperty AGENT_GROUPS = ArchaiusUtil.getString("agent.groups");
    public static final String WILDCARD = "*";
    public static final String UNASSIGNED = "!";

    ObjectManager objectManager;
    Set<String> groups;
    Set<Long> groupLongs;

    @Override
    public boolean shouldHandle(Agent agent) {
        return agent == null ? false : shouldHandleGroup(agent.getAgentGroupId());
    }


    @Override
    public boolean shouldHandle(long agentId) {
        return shouldHandle(objectManager.loadResource(Agent.class, agentId));
    }


    @Override
    public boolean shouldHandleGroup(AgentGroup group) {
        return group == null ? shouldHandleGroup((Long)null) : shouldHandle(group.getId());
    }

    @Override
    public boolean shouldHandleGroup(Long agentGroupId) {
        if ( shouldHandleWildcard() ) {
            return true;
        }

        if ( agentGroupId == null ) {
            return shouldHandleUnassigned();
        } else {
            return groups.contains(agentGroupId.toString());
        }
    }

    @Override
    public boolean shouldHandleWildcard() {
        return groups.contains(WILDCARD);
    }

    @Override
    public boolean shouldHandleUnassigned() {
        return groups.contains(UNASSIGNED);
    }

    @Override
    public Set<Long> supportedGroups() {
        return groupLongs;
    }

    @PostConstruct
    public void init() {
        load(false);
    }

    protected void load(boolean initial) {
        Set<String> groups = new HashSet<String>();
        Set<Long> groupLongs = new HashSet<Long>();
        for ( String group : AGENT_GROUPS.get().trim().split("\\s*,\\s*") ) {
            groups.add(group);
            try {
                groupLongs.add(new Long(group));
            } catch ( NumberFormatException nfe ) {
            }
        }

        this.groups = groups;
        this.groupLongs = groupLongs;

        if ( initial ) {
            AGENT_GROUPS.addCallback(new Runnable() {
                @Override
                public void run() {
                    load(false);
                }
            });
        }
    }


    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }


}
