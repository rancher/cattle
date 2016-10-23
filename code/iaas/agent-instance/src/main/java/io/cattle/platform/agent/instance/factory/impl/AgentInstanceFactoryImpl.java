package io.cattle.platform.agent.instance.factory.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.factory.AgentInstanceBuilder;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.agent.instance.factory.lock.AgentInstanceAgentCreateLock;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;

public class AgentInstanceFactoryImpl implements AgentInstanceFactory {

    @Inject
    AccountDao accountDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    AgentInstanceDao factoryDao;
    @Inject
    LockManager lockManager;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    AgentLocator agentLocator;
    @Inject
    ResourceMonitor resourceMonitor;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    InstanceDao instanceDao;

    @Override
    public AgentInstanceBuilder newBuilder() {
        return new AgentInstanceBuilderImpl(this);
    }

    public Long getAgentGroupId(Instance instance) {
        for (Host host : objectManager.mappedChildren(instance, Host.class)) {
            RemoteAgent remoteAgent = agentLocator.lookupAgent(host);
            if (remoteAgent != null) {
                Agent agent = objectManager.loadResource(Agent.class, remoteAgent.getAgentId());
                return agent.getAgentGroupId();
            }
        }

        return null;
    }

    protected Instance build(AgentInstanceBuilderImpl builder) {
        Agent agent = getAgent(builder);

        return getInstance(agent, builder);
    }

    private Instance getInstance(Agent agent, AgentInstanceBuilderImpl builder) {
        Instance instance = factoryDao.getInstanceByAgent(agent);

        if (instance != null) {
            return instance;
        }

        Map<String, Object> properties = getInstanceProperties(agent, builder);

        return createInstance(agent, properties, builder);
    }

    private Map<String, Object> getInstanceProperties(Agent agent, AgentInstanceBuilderImpl builder) {
        Map<Object, Object> properties = new HashMap<Object, Object>();

        properties.put(INSTANCE.ACCOUNT_ID, getAccountId(builder));
        properties.put(INSTANCE.AGENT_ID, agent.getId());
        properties.put(InstanceConstants.FIELD_IMAGE_UUID, builder.getImageUuid());
        properties.put(INSTANCE.NAME, builder.getName());
        properties.put(INSTANCE.ZONE_ID, agent.getZoneId());
        properties.put(INSTANCE.KIND, builder.getInstanceKind());
        properties.put(INSTANCE.SYSTEM_CONTAINER, builder.getSystemContainerType());
        properties.put(InstanceConstants.FIELD_INSTANCE_TRIGGERED_STOP, builder.getInstanceTriggeredStop());
        properties.put(InstanceConstants.FIELD_PRIVILEGED, builder.isPrivileged());
        properties.put(InstanceConstants.FIELD_VNET_IDS, getVnetIds(agent, builder));
        properties.put(InstanceConstants.FIELD_NETWORK_IDS, getNetworkIds(agent, builder));
        properties.putAll(builder.getParams());
        addAdditionalProperties(properties, agent, builder);

        return objectManager.convertToPropertiesFor(Instance.class, properties);
    }

    protected void addAdditionalProperties(Map<Object, Object> properties, Agent agent, AgentInstanceBuilderImpl builder) {
    }

    protected Object getVnetIds(Agent agent, AgentInstanceBuilderImpl builder) {
        Long vnetId = builder.getVnetId();
        return vnetId == null ? null : new Long[] { builder.getVnetId() };
    }

    @Override
    public Agent createAgent(Instance instance) {
        if (shouldCreateAgent(instance)) {
            Map<String, Object> accountData = new HashMap<>();
            Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
            if (AgentConstants.ENVIRONMENT_ROLE.equals(labels.get(SystemLabels.LABEL_AGENT_ROLE))) {
                accountData = CollectionUtils.asMap(AccountConstants.DATA_ACT_AS_RESOURCE_ACCOUNT, true);
            } else if (AgentConstants.ENVIRONMENT_ADMIN_ROLE.equals(labels.get(SystemLabels.LABEL_AGENT_ROLE))) {
                // allow to set this flag only for system services
                List<? extends Service> services = instanceDao.findServicesNonRemovedLinksOnly(instance);
                for (Service service : services) {
                    Stack stack = objectManager.loadResource(Stack.class, service.getStackId());
                    if (ServiceConstants.isSystem(stack)) {
                        accountData = CollectionUtils.asMap(AccountConstants.DATA_ACT_AS_RESOURCE_ADMIN_ACCOUNT, true);
                        break;
                    }
                }
            }

            return getAgent(new AgentInstanceBuilderImpl(this, instance, accountData));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected boolean isSystemService(Service service) {
        Stack stack = objectManager.loadResource(Stack.class, service.getStackId());
        if (DataAccessor.fieldBool(stack, "isSystem")) {
            return true;
        }
        
        Map<String, Object> data = DataAccessor.fields(service)
        .withKey("launchConfig").withDefault(Collections.EMPTY_MAP)
                .as(Map.class);
        
        Object labelsObj = data.get(InstanceConstants.FIELD_LABELS);
        if (labelsObj == null) {
            return false;
        }

        return Boolean.valueOf(service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE));
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

        if (CommonStatesConstants.DEACTIVATING.equals(agent.getState())) {
            return;
        }

        try {
            processManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, agent,
                    ProcessUtils.chainInData(new HashMap<String, Object>(), AgentConstants.PROCESS_DEACTIVATE, AgentConstants.PROCESS_REMOVE));
        } catch (ProcessCancelException e) {
            try {
                processManager.scheduleStandardProcess(StandardProcess.REMOVE, agent, null);
            } catch (ProcessCancelException e1) {
            }
        }
    }

    protected boolean shouldCreateAgent(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        return BooleanUtils.toBoolean(ObjectUtils.toString(labels.get(SystemLabels.LABEL_AGENT_CREATE), null));
    }

    protected Agent getAgent(AgentInstanceBuilderImpl builder) {
        String uri = getUri(builder);
        Agent agent = factoryDao.getAgentByUri(uri);

        if (agent == null) {
            agent = createAgent(uri, builder);
        }

        agent = resourceMonitor.waitFor(agent, new ResourcePredicate<Agent>() {
            @Override
            public boolean evaluate(Agent obj) {
                return factoryDao.getActivateCredentials(obj).size() > 0;
            }

            @Override
            public String getMessage() {
                return "active credentials";
            }
        });

        return agent;
    }

    protected Instance createInstance(final Agent agent, final Map<String, Object> properties, final AgentInstanceBuilderImpl builder) {
        return lockManager.lock(new AgentInstanceAgentCreateLock(agent.getUri()), new LockCallback<Instance>() {
            @Override
            public Instance doWithLock() {
                Instance instance = factoryDao.getInstanceByAgent(agent);

                if (instance == null) {
                    instance = DeferredUtils.nest(new Callable<Instance>() {
                        @Override
                        public Instance call() throws Exception {
                            return factoryDao.createInstanceForProvider(builder.getNetworkServiceProvider(), properties);
                        }
                    });
                }

                return instance;
            }
        });
    }

    protected Agent createAgent(final String uri, final AgentInstanceBuilderImpl builder) {
        return lockManager.lock(new AgentInstanceAgentCreateLock(uri), new LockCallback<Agent>() {
            @Override
            public Agent doWithLock() {
                Agent agent = factoryDao.getAgentByUri(uri);
                final Map<String, Object> data = new HashMap<>();
                if (builder.getResourceAccountId() != null) {
                    data.put(AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID, builder.getResourceAccountId());
                }
                if (builder.getAccountData() != null) {
                    data.put(AgentConstants.DATA_ACCOUNT_DATA, builder.getAccountData());
                }

                if (agent == null) {
                    agent = DeferredUtils.nest(new Callable<Agent>() {
                        @Override
                        public Agent call() throws Exception {
                            return resourceDao.createAndSchedule(Agent.class,
                                    AGENT.DATA, data,
                                    AGENT.URI, uri,
                                    AGENT.MANAGED_CONFIG, builder.isManagedConfig(),
                                    AGENT.AGENT_GROUP_ID, builder.getAgentGroupId(),
                                    AGENT.ZONE_ID, builder.getZoneId());
                        }
                    });
                }

                return agent;
            }
        });
    }

    protected Long getAccountId(AgentInstanceBuilderImpl builder) {
        if (builder.isAccountOwned() && builder.getAccountId() != null) {
            return builder.getAccountId();
        }

        return accountDao.getSystemAccount().getId();
    }

    protected String getUri(AgentInstanceBuilderImpl builder) {
        if (builder.getUri() != null) {
            return builder.getUri();
        }

        Long networkServiceProviderId = builder.getNetworkServiceProvider() == null ? null : builder.getNetworkServiceProvider().getId();

        return String.format("delegate:///?vnetId=%d&networkServiceProviderId=%d", builder.getVnetId(), networkServiceProviderId);
    }

    @SuppressWarnings("unchecked")
    protected List<Long> getNetworkIds(Agent agent, AgentInstanceBuilderImpl builder) {
        List<Long> networkIds = new ArrayList<>();
        if (builder.getParams().get(InstanceConstants.FIELD_NETWORK_IDS) != null) {
            networkIds = (List<Long>) builder.getParams().get(InstanceConstants.FIELD_NETWORK_IDS);
        }
        return networkIds;
    }

}
