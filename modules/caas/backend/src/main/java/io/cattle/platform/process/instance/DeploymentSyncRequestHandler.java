package io.cattle.platform.process.instance;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.DeploymentSyncResponse;
import io.cattle.platform.core.addon.InstanceStatus;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.metadata.MetadataManager;
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

    public static final DynamicStringProperty EXTERNAL_STYLE = ArchaiusUtil.getString("external.compute.event.target");

    DeploymentSyncFactory syncFactory;
    MetadataManager metadataManager;
    boolean externalAlways;

    public DeploymentSyncRequestHandler(AgentLocator agentLocator, ObjectSerializer serializer,
                                        ObjectManager objectManager, ObjectProcessManager processManager,
                                        DeploymentSyncFactory syncFactory,
                                        MetadataManager metadataManager) {
        super(agentLocator, serializer, objectManager, processManager);
        this.syncFactory = syncFactory;
        this.metadataManager = metadataManager;
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
            if ("container".equals(EXTERNAL_STYLE.get())) {
                Set<Long> agentIds = new TreeSet<>(metadataManager.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_COMPUTE,
                        instance.getAccountId()));
                return agentIds.iterator().next();
            } else if ("host".equals(EXTERNAL_STYLE.get())) {
                long agentId = Long.MAX_VALUE;
                for (HostInfo info : metadataManager.getMetadataForAccount(instance.getAccountId()).getHosts()) {
                    if (CommonStatesConstants.ACTIVE.equals(info.getAgentState()) &&
                            CommonStatesConstants.ACTIVE.equals(info.getState()) &&
                            info.getAgentId() != null &&
                            info.getAgentId() < agentId) {
                        agentId = info.getAgentId();
                    }
                }

                if (agentId != Long.MAX_VALUE) {
                    return agentId;
                }
            }
        } catch (NoSuchElementException e) {
        }

        throw new ExecutionErrorException("Failed to find external agent provider", instance);
    }

    @Override
    protected void preProcessEvent(EventVO<?, ?> event, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource, Object agentResource) {
        super.preProcessEvent(event, state, process, eventResource, dataResource, agentResource);

        // Long means that we are going to an agent, not instance. So it's external. A bit hacky.
        if (agentResource instanceof Long) {
            event.setName("external." + event.getName());
        }
    }

    @Override
    protected Map<Object, Object> getResourceDataMap(EventVO<?, ?> event, Event reply, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource, Object agentResource) {
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
                    HostInfo host = metadataManager.getMetadataForAccount(instance.getAccountId()).getHost(status.getHostUuid());
                    if (host != null) {
                        data.put(INSTANCE.HOST_ID, host.getId());
                    }
                }
            }
        }

        return data;
    }

}
