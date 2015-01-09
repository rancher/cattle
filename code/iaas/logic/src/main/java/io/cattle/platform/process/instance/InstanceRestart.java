package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class InstanceRestart extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        DataAccessor.fromMap(state.getData()).withScope(InstanceProcessOptions.class).withKey(InstanceProcessOptions.START).set(true);

        return new HandlerResult().withChainProcessName(InstanceConstants.PROCESS_STOP);
    }

}
