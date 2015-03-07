package io.cattle.platform.metadata.process;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.metadata.service.MetadataService;
import io.cattle.platform.metadata.util.MetadataConstants;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.CollectionUtils;

import javax.inject.Inject;

public class MetadataAttachProcessHandler extends AbstractObjectProcessLogic implements ProcessPreListener {

    MetadataService metadataService;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceHostMap map = (InstanceHostMap) state.getResource();
        Instance instance = objectManager.loadResource(Instance.class, map.getInstanceId());

        if (metadataService.isAttachMetadata(instance)) {
            objectManager.setFields(instance, ObjectMetaDataManager.APPEND + ObjectMetaDataManager.DATA_FIELD, CollectionUtils.asMap(
                    MetadataConstants.METADATA_ATTACH, true));
        }

        return null;
    }

    public MetadataService getMetadataService() {
        return metadataService;
    }

    @Inject
    public void setMetadataService(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

}
