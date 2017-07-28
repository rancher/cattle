package io.cattle.platform.process.instance;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.addon.DeploymentSyncResponse;
import io.cattle.platform.core.addon.InstanceStatus;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.environment.EnvironmentResourceManager;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.metadata.model.HostInfo;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.util.exception.ExecutionErrorException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import static io.cattle.platform.core.model.Tables.*;

public class DeploymentSyncRequestHandler extends AgentBasedProcessHandler {

    DeploymentSyncFactory syncFactory;
    EnvironmentResourceManager envResourceManager;
    boolean externalAlways;

    public DeploymentSyncRequestHandler(AgentLocator agentLocator, ObjectSerializer serializer,
                                        ObjectManager objectManager, ObjectProcessManager processManager,
                                        DeploymentSyncFactory syncFactory,
                                        EnvironmentResourceManager envResourceManager) {
        super(agentLocator, serializer, objectManager, processManager);
        this.syncFactory = syncFactory;
        this.envResourceManager = envResourceManager;
    }

    @Override
    protected Object getDataResource(ProcessState state, ProcessInstance process) {
        return syncFactory.construct((Instance) state.getResource());
    }

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Instance instance = (Instance)state.getResource();
        if (!DataAccessor.fieldBool(instance, InstanceConstants.FIELD_EXTERNAL_COMPUTE_AGENT)) {
            return super.getAgentResource(state, process, dataResource);
        }

        if (instance.getHostId() != null && !externalAlways) {
            return super.getAgentResource(state, process, dataResource);
        }

        try {
            Set<Long> agentIds = new TreeSet<>(envResourceManager.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_COMPUTE,
                    instance.getAccountId()));
            return agentIds.iterator().next();
        } catch (NoSuchElementException e) {

            throw new ExecutionErrorException("Failed to find external agent provider", instance);
        }
    }

    @Override
    protected Map<Object, Object> getResourceDataMap(EventVO<?> event, Event reply, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource, Object agentResource) {
        Map<Object, Object> data = new HashMap<>();

        Instance instance = (Instance)state.getResource();
        DeploymentSyncResponse response = syncFactory.getResponse(reply);
        if (response != null) {
            for (InstanceStatus status : response.getInstanceStatus()) {
                if (!instance.getUuid().equals(status.getInstanceUuid())) {
                    continue;
                }

                if (StringUtils.isNotBlank(status.getExternalId())) {
                    data.put(INSTANCE.EXTERNAL_ID, status.getExternalId());
                }

                if (status.getDockerInspect() != null) {
                    data.put(InstanceConstants.FIELD_DOCKER_INSPECT, status.getDockerInspect());
                }

                if (status.getHostUuid() != null && instance.getHostId() == null) {
                    HostInfo host = envResourceManager.getMetadata(instance.getAccountId()).getHost(status.getHostUuid());
                    if (host != null) {
                        data.put(INSTANCE.HOST_ID, host.getId());
                    }
                }
            }
        }

        return data;
    }

}
