package io.cattle.platform.docker.process.instancehostmap;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Named;

@Named
public class DockerPostInstanceHostMapActivatePreListener extends AbstractObjectProcessLogic implements
        ProcessPreListener,
        Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceHostMap map = (InstanceHostMap) state.getResource();
        Instance instance = getObjectManager().loadResource(Instance.class, map.getInstanceId());
        if (instance == null) {
            return null;
        }
        Integer currentCount = DataAccessor.fieldInteger(instance, InstanceConstants.FIELD_START_RETRY_COUNT);
        Integer retryCount = currentCount == null ? 1 : currentCount + 1;
        objectManager.setFields(instance, InstanceConstants.FIELD_START_RETRY_COUNT,
                retryCount);
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
