package io.cattle.platform.process.stack;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Named;

@Named
public class StackPostUpgrade extends AbstractObjectProcessHandler implements ProcessPostListener {

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_STACK_UPGRADE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        return new HandlerResult().withChainProcessName(ServiceConstants.PROCESS_STACK_FINISH_UPGRADE);
    }

}