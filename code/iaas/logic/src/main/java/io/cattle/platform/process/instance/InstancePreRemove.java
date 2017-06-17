package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lifecycle.InstanceLifecycleManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstancePreRemove extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    InstanceLifecycleManager instanceLifecycle;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        instanceLifecycle.preRemove(instance);
        return null;
    }

}
