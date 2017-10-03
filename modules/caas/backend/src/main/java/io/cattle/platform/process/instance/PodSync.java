package io.cattle.platform.process.instance;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.addon.DeploymentSyncResponse;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.process.common.handler.ExternalProcessHandler;
import io.cattle.platform.util.exception.ExecutionException;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static io.cattle.platform.core.model.Tables.*;

public class PodSync extends ExternalProcessHandler
{
    DeploymentSyncFactory syncFactory;
    MetadataManager metadataManager;

    public PodSync(EventService eventService, ObjectManager objectManager, ObjectProcessManager objectProcessManager, ObjectMetaDataManager objectMetaDataManager, DeploymentSyncFactory syncFactory, ObjectSerializer objectSerializer, MetadataManager metadataManager) {
        super("k8s-cluster-service", eventService, objectManager, objectProcessManager, objectMetaDataManager, objectSerializer);
        this.metadataManager = metadataManager;
        this.syncFactory = syncFactory;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object noOp = state.getData().get(InstanceConstants.PROCESS_DATA_NO_OP);
        if (noOp instanceof Boolean && (Boolean) noOp) {
            return null;
        }

        Instance instance = (Instance)state.getResource();
        if (instance.getNativeContainer() || !instance.getDesired()) {
            return null;
        }

        if (!InstanceConstants.isKubernetes(instance)) {
            return null;
        }

        if (instance.getExternalId() == null) {
            state.getData().put(InstanceConstants.PROCESS_DATA_NO_OP, true);
        }

        return super.handle(state, process);
    }

    @Override
    protected String getEventName(ProcessInstance process) {
        return "deploymentunit.sync";
    }

    @Override
    public HandlerResult complete(ListenableFuture<?> future, ProcessState state, ProcessInstance process) {
        try {
            AsyncUtils.get(future);
            return super.complete(future, state, process);
        } catch (ExecutionException e) {
            return InstanceProcessManager.handleStartError(processManager, state, (Instance)state.getResource(), e);
        }
    }

    @Override
    protected Object getData(ProcessState state, ProcessInstance process) {
        return objectSerializer.serialize(syncFactory.construct((Instance)state.getResource()));
    }

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> data) {
        Instance instance = (Instance)state.getResource();
        DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, instance.getDeploymentUnitId());
        DeploymentSyncResponse response = syncFactory.getResponse(data);

        if (unit != null && StringUtils.isNotBlank(response.getExternalId())) {
            objectManager.setFields(unit,
                    DEPLOYMENT_UNIT.EXTERNAL_ID, response.getExternalId());
        }

        return new HandlerResult(DeploymentSyncRequestHandler.getResourceDataMap(metadataManager, response, instance));
    }
}
