package io.cattle.platform.process.common.handler;

import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;

public abstract class AbstractObjectProcessPrePostListener extends AbstractObjectProcessLogic implements ProcessPreListener, ProcessPostListener {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        switch (state.getPhase()) {
        case PRE_LISTENERS:
            return preHandle(state, process);
        case POST_LISTENERS:
            return postHandle(state, process);
        default:
            break;
        }

        return null;
    }

    protected abstract HandlerResult preHandle(ProcessState state, ProcessInstance process);

    protected abstract HandlerResult postHandle(ProcessState state, ProcessInstance process);

}
