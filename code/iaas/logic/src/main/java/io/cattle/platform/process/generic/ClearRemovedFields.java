package io.cattle.platform.process.generic;

import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Named;

@Named
public class ClearRemovedFields extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        if (!ObjectUtils.hasWritableProperty(resource, ObjectMetaDataManager.REMOVED_FIELD)) {
            return null;
        }

        return new HandlerResult(ObjectMetaDataManager.REMOVED_FIELD, null, ObjectMetaDataManager.REMOVE_TIME_FIELD, null);
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "*.restore" };
    }

}
