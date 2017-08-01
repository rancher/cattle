package io.cattle.platform.process.instance;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.lifecycle.impl.K8sLifecycleManagerImpl;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.util.exception.ExecutionErrorException;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;
import java.util.Map;

public class K8sProviderLabels extends AgentBasedProcessHandler {

    EnvironmentResourceManager envResourceManager;

    public K8sProviderLabels(AgentLocator agentLocator, ObjectSerializer serializer, ObjectManager objectManager, ObjectProcessManager processManager,
                             EnvironmentResourceManager envResourceManager) {
        super(agentLocator, serializer, objectManager, processManager);
        this.envResourceManager = envResourceManager;

        commandName = "compute.instance.providelables";
        timeoutIsError = true;
        eventRetry = 1;
    }

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Instance instance = getInstance(state);
        if (!isPod(instance)) {
            return null;
        }
        Long accountId = instance.getAccountId();
        List<Long> agentIds = envResourceManager.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_LABELS_PROVIDER, accountId);
        Long agentId = agentIds.size() == 0 ? null : agentIds.get(0);
        if ((instance instanceof Instance) && (agentIds.contains(instance.getAgentId()) || instance.getSystem())) {
            return null;
        }

        if (agentId == null) {
            if (k8sRequired(instance)) {
                throw new ExecutionErrorException("Failed to find labels provider", instance);
            } else {
                return null;
            }
        }

        return agentId;
    }

    protected boolean k8sRequired(Instance instance) {
        Account account = objectManager.loadResource(Account.class, instance.getAccountId());
        String compute = DataAccessor.fieldString(account, AccountConstants.FIELD_COMPUTE_FLAVOR);
        return AccountConstants.isFlavorKubernetes(compute);
    }

    protected boolean isPod(Instance instance) {
        Map<String, String> labels = DataAccessor.getLabels(instance);
        String namespace = labels.get(K8sLifecycleManagerImpl.POD_NAMESPACE);
        String name = labels.get(K8sLifecycleManagerImpl.POD_NAME);
        String containerName = labels.get(K8sLifecycleManagerImpl.CONTAINER_NAME);
        if (namespace == null || name == null) {
            return false;
        }

        return K8sLifecycleManagerImpl.POD.equals(containerName);
    }

    protected Instance getInstance(ProcessState state) {
        return (Instance) state.getResource();
    }

    @Override
    protected void preProcessEvent(EventVO<?> event, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource,
            Object agentResource) {
        Map<String, Object> data = CollectionUtils.toMap(event.getData());
        if (!data.containsKey("instance")) {
            Object instanceData = CollectionUtils.getNestedValue(data, "instanceHostMap", "instance");
            data.put("instance", instanceData);
        }
        super.preProcessEvent(event, state, process, eventResource, dataResource, agentResource);
    }

    @Override
    protected void postProcessEvent(EventVO<?> event, Event reply, ProcessState state, ProcessInstance process,
            Object eventResource, Object dataResource, Object agentResource) {
        Map<String, String> labels = CollectionUtils.toMap(CollectionUtils.getNestedValue(reply.getData(),
                "instance", "+data", "+fields", "+labels"));
        if (labels.size() == 0) {
            labels = CollectionUtils.toMap(CollectionUtils.getNestedValue(reply.getData(),
                    "instanceHostMap", "instance", "+data", "+fields", "+labels"));
        } else {
            CollectionUtils.setNestedValue(CollectionUtils.toMap(reply.getData()), labels,
                    "instanceHostMap", "instance", "+data", "+fields", "+labels");
        }

        Instance instance = getInstance(state);

        if (labels.size() == 0) {
            throw new ExecutionException("Failed to find labels for POD", instance);
        }
    }
}