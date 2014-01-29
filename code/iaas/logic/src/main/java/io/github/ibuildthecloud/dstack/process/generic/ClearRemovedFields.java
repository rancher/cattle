package io.github.ibuildthecloud.dstack.process.generic;

import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPostListener;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.util.ObjectUtils;
import io.github.ibuildthecloud.dstack.process.common.handler.AbstractObjectProcessLogic;
import io.github.ibuildthecloud.dstack.util.type.Priority;

import javax.inject.Named;

@Named
public class ClearRemovedFields extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        if ( ! ObjectUtils.hasWritableProperty(resource, ObjectMetaDataManager.REMOVED_FIELD) ) {
            return null;
        }

        return new HandlerResult(
                ObjectMetaDataManager.REMOVED_FIELD, null,
                ObjectMetaDataManager.REMOVE_TIME_FIELD, null);
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

