package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceLinkConstants;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstancePurge extends AbstractDefaultProcessHandler {

    @Inject
    VolumeDao volumeDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();

        for (Port port : getObjectManager().children(instance, Port.class)) {
            deactivateThenRemove(port, state.getData());
        }

        for (InstanceLink link : getObjectManager().children(instance, InstanceLink.class, InstanceLinkConstants.FIELD_INSTANCE_ID)) {
            deactivateThenRemove(link, state.getData());
        }

        for (InstanceLink link : getObjectManager().children(instance, InstanceLink.class, InstanceLinkConstants.FIELD_TARGET_INSTANCE_ID)) {
            objectManager.setFields(link, InstanceLinkConstants.FIELD_TARGET_INSTANCE_ID, (Object)null);
        }

        objectManager.reload(instance);

        return null;
    }
}
