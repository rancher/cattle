package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.process.common.handler.ExternalProcessHandler;

public abstract class PodHandler extends ExternalProcessHandler {

    DeploymentSyncFactory syncFactory;

    public PodHandler(EventService eventService, ObjectManager objectManager, ObjectProcessManager objectProcessManager, ObjectMetaDataManager objectMetaDataManager, DeploymentSyncFactory syncFactory, ObjectSerializer objectSerializer) {
        super("k8s-cluster-service", eventService, objectManager, objectProcessManager, objectMetaDataManager, objectSerializer);
        this.syncFactory = syncFactory;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!InstanceConstants.isKubernetes(instance)) {
            return null;
        }

        return super.handle(state, process);
    }

    @Override
    protected Object getData(ProcessState state, ProcessInstance process) {
        return objectSerializer.serialize(syncFactory.construct((Instance)state.getResource()));
    }

}
