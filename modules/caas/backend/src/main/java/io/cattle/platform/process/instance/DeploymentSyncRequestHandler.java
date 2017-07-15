package io.cattle.platform.process.instance;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.core.addon.DeploymentSyncResponse;
import io.cattle.platform.core.addon.InstanceStatus;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static io.cattle.platform.core.model.Tables.*;

public class DeploymentSyncRequestHandler extends AgentBasedProcessHandler {

    DeploymentSyncFactory syncFactory;

    public DeploymentSyncRequestHandler(AgentLocator agentLocator, ObjectSerializer serializer, ObjectManager objectManager, ObjectProcessManager processManager, DeploymentSyncFactory syncFactory) {
        super(agentLocator, serializer, objectManager, processManager);
        this.syncFactory = syncFactory;
    }

    @Override
    protected Object getDataResource(ProcessState state, ProcessInstance process) {
        return syncFactory.construct((Instance) state.getResource());
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
            }
        }

        return data;
    }

}
