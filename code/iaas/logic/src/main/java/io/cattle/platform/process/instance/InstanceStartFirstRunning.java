package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.Date;

import javax.inject.Named;

@Named
public class InstanceStartFirstRunning extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.start" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Date running = ((Instance) state.getResource()).getFirstRunning();
        return new HandlerResult(INSTANCE.FIRST_RUNNING, running == null ? new Date() : running);
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

}
