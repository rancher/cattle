package io.cattle.platform.process.instance;

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
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class PodCreate extends PodHandler
{
    MetadataManager metadataManager;

    public PodCreate(EventService eventService, ObjectManager objectManager, ObjectProcessManager objectProcessManager, ObjectMetaDataManager objectMetaDataManager, DeploymentSyncFactory syncFactory, ObjectSerializer objectSerializer, MetadataManager metadataManager) {
        super(eventService, objectManager, objectProcessManager, objectMetaDataManager, syncFactory, objectSerializer);
        this.metadataManager = metadataManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (instance.getNativeContainer() || StringUtils.isNotBlank(instance.getExternalId())) {
            return null;
        }
        return super.handle(state, process);
    }

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> data) {
        return new HandlerResult(DeploymentSyncRequestHandler.getResourceDataMap(metadataManager, syncFactory, (Instance)state.getResource(), data));
    }
}
