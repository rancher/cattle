package io.cattle.platform.process.instance;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.DeploymentSyncResponse;
import io.cattle.platform.core.addon.InstanceStatus;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static io.cattle.platform.core.model.Tables.*;

public class DeploymentSyncRequestHandler extends AgentBasedProcessHandler {

    public static final DynamicStringProperty EXTERNAL_STYLE = ArchaiusUtil.getString("external.compute.event.target");

    DeploymentSyncFactory syncFactory;
    MetadataManager metadataManager;

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
    protected void preProcessEvent(EventVO<?, ?> event, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource, Object agentResource) {
        super.preProcessEvent(event, state, process, eventResource, dataResource, agentResource);
    }

    @Override
    protected Map<Object, Object> getResourceDataMap(EventVO<?, ?> event, Event reply, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource, Object agentResource) {
        return getResourceDataMap(metadataManager, syncFactory, (Instance)state.getResource(), CollectionUtils.toMap(reply.getData()));

    }

    public static Map<Object, Object> getResourceDataMap(MetadataManager metadataManager, DeploymentSyncFactory syncFactory, Instance instance, Map<Object, Object> replyData) {
        Map<Object, Object> data = new HashMap<>();

        DeploymentSyncResponse response = syncFactory.getResponse(replyData);
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

                if (instance.getHostId() == null && StringUtils.isNotBlank(response.getNodeName())) {
                    HostInfo host = metadataManager.getMetadataForAccount(instance.getAccountId()).getHostByNodeName(response.getNodeName());
                    if (host != null) {
                        data.put(INSTANCE.HOST_ID, host.getId());
                    }
                }

                if (StringUtils.isNotBlank(status.getPrimaryIpAddress())) {
                    data.put(InstanceConstants.FIELD_PRIMARY_IP_ADDRESS, status.getPrimaryIpAddress());
                }
            }
        }

        return data;
    }

}
