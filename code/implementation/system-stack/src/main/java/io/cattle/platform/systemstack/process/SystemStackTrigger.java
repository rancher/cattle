package io.cattle.platform.systemstack.process;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

public class SystemStackTrigger implements ProcessHandler {
    public static final String STACKS = "system-stacks";

    ConfigItemStatusManager itemManager;
    ObjectManager objectManager;

    public SystemStackTrigger(ConfigItemStatusManager itemManager, ObjectManager objectManager) {
        super();
        this.itemManager = itemManager;
        this.objectManager = objectManager;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();

        if (resource instanceof Stack) {
            Stack stack = (Stack)resource;
            if (!ServiceConstants.isSystem(stack)) {
                Account account = objectManager.loadResource(Account.class, stack.getAccountId());
                if (DataAccessor.fieldString(account, AccountConstants.FIELD_ORCHESTRATION) != null) {
                    return null;
                }
            }
            trigger(stack.getAccountId());
        } else if (resource instanceof Host) {
            trigger(((Host) resource).getAccountId());
        }

        return null;
    }

    public void trigger(Long accountId) {
        ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Account.class, accountId);
        request.addItem(STACKS);
        request.withDeferredTrigger(true);
        itemManager.updateConfig(request);
    }

}
