package io.cattle.platform.agent.instance.factory.impl;

import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import org.apache.commons.lang3.BooleanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static io.cattle.platform.core.model.tables.AgentTable.*;

public class AgentInstanceFactoryImpl implements AgentInstanceFactory {

    ObjectManager objectManager;
    AgentDao agentDao;
    GenericResourceDao resourceDao;
    ResourceMonitor resourceMonitor;
    ObjectProcessManager processManager;

    public AgentInstanceFactoryImpl(ObjectManager objectManager, AgentDao agentDao, GenericResourceDao resourceDao, ResourceMonitor resourceMonitor,
            ObjectProcessManager processManager) {
        super();
        this.objectManager = objectManager;
        this.agentDao = agentDao;
        this.resourceDao = resourceDao;
        this.resourceMonitor = resourceMonitor;
        this.processManager = processManager;
    }

    @Override
    public Agent createAgent(Instance instance) {
        if (!shouldCreateAgent(instance)) {
            return null;
        }

        Set<String> filteredRoles = new HashSet<>();

        String rolesVal = DataAccessor.getLabel(instance, SystemLabels.LABEL_AGENT_ROLE);
        if (rolesVal != null) {
            filteredRoles = Arrays.stream(rolesVal.split(","))
                    .filter(AgentConstants.CREATE_ROLES::contains)
                    .collect(Collectors.toSet());
        }

        return getAgent(new AgentBuilderRequest(instance, filteredRoles));
    }

    @Override
    public void deleteAgent(Instance instance) {
        if (!shouldCreateAgent(instance) || instance.getAgentId() == null) {
            return;
        }

        Agent agent = objectManager.loadResource(Agent.class, instance.getAgentId());
        if (agent == null) {
            return;
        }

        processManager.deactivateThenRemove(agent, null);
    }

    protected boolean shouldCreateAgent(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        return BooleanUtils.toBoolean(Objects.toString(labels.get(SystemLabels.LABEL_AGENT_CREATE), null));
    }

    protected Agent getAgent(AgentBuilderRequest builder) {
        String uri = builder.getUri();
        Agent agent = agentDao.findNonRemovedByUri(uri);

        if (agent == null) {
            agent = createAgent(uri, builder);
        }

        return agent;
    }

    @Override
    public boolean areAllCredentialsActive(Agent agent) {
        return agentDao.areAllCredentialsActive(agent);
    }

    protected Agent createAgent(final String uri, final AgentBuilderRequest builder) {
        Agent agent = agentDao.findNonRemovedByUri(uri);
        Map<String, Object> data = new HashMap<>();

        if (builder.getRequestedRoles() != null) {
            data.put(AgentConstants.DATA_REQUESTED_ROLES, new ArrayList<>(builder.getRequestedRoles()));
        }

        if (agent != null) {
            return agent;
        }

        return DeferredUtils.nest(new Callable<Agent>() {
            @Override
            public Agent call() throws Exception {
                return resourceDao.createAndSchedule(Agent.class,
                        AGENT.DATA, data,
                        AGENT.URI, uri,
                        AGENT.RESOURCE_ACCOUNT_ID, builder.getResourceAccountId(),
                        AGENT.MANAGED_CONFIG, false);
            }
        });
    }

}
