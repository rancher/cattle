package io.cattle.platform.systemstack.process;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;

public class SystemStackTrigger extends AbstractObjectProcessLogic implements ProcessPostListener, ProcessPreListener {
    public static final String STACKS = "system-stacks";

    @Inject
    ConfigItemStatusManager itemManager;

    @Override
    public String[] getProcessNames() {
        return new String[]{
            "stack.*",
        };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Stack stack = (Stack)state.getResource();
        if (!DataAccessor.fieldBool(stack, ServiceConstants.FIELD_SYSTEM)) {
            return null;
        }

        ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Account.class, stack.getAccountId());
        request.addItem(STACKS);
        request.withDeferredTrigger(true);
        itemManager.updateConfig(request);
        return null;
    }

}