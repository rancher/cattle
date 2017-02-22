package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Named;

@Named
public class InstanceRemoveChainPurgePostHandler extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        
        return new HandlerResult().withChainProcessName(InstanceConstants.PROCESS_PURGE);
    }

    @Override
    public int getPriority() {
        // run last
        return Integer.MAX_VALUE;
    }
}
