package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Named;

@Named
public class InstanceStartCountUpdatePostHandler extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceHostMap map = (InstanceHostMap) state.getResource();
        Instance instance = getObjectManager().loadResource(Instance.class, map.getInstanceId());
        if (instance == null) {
            return null;
        }

        // set startCount
        Long startCount = instance.getStartCount() == null ? 1 : instance.getStartCount() + 1;
        objectManager.setFields(instance, INSTANCE.START_COUNT, startCount);
        return null;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}

