package io.cattle.platform.process.common.handler;

import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.ObjectUtils;

public abstract class AbstractGenericObjectProcessLogic extends AbstractObjectProcessLogic {

    public abstract String getKind();

    @Override
    public final HandlerResult handle(ProcessState state, ProcessInstance process) {
        if (getKind().equals(ObjectUtils.getKind(state.getResource()))) {
            return handleKind(state, process);
        }

        return null;
    }

    protected abstract HandlerResult handleKind(ProcessState state, ProcessInstance process);

}
