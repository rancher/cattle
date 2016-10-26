package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Named;

@Named
public class InstanceStopPostAction extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.stop" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();

        String chainProcess = null;
        boolean start = DataAccessor.fromMap(state.getData()).withScope(InstanceProcessOptions.class).withKey(InstanceProcessOptions.START).withDefault(false)
                .as(Boolean.class);

        if (start) {
            chainProcess = InstanceConstants.PROCESS_START;
        } else if (Boolean.TRUE.equals(state.getData().get(InstanceConstants.REMOVE_OPTION)) ||
                InstanceConstants.ON_STOP_REMOVE.equals(instance.getInstanceTriggeredStop())) {
            chainProcess = objectProcessManager.getStandardProcessName(StandardProcess.REMOVE, instance);
        }

        return new HandlerResult().withChainProcessName(chainProcess);
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
