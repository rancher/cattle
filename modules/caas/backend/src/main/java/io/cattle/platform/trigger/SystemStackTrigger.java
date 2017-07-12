package io.cattle.platform.trigger;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

public class SystemStackTrigger implements Trigger {
    public static final String STACKS = "system-stacks";

    LoopManager loopManager;
    ObjectManager objectManager;

    public SystemStackTrigger(LoopManager loopManager, ObjectManager objectManager) {
        super();
        this.loopManager = loopManager;
        this.objectManager = objectManager;
    }

    @Override
    public void trigger(Long accountId, Object resource, String source) {
        if (resource instanceof Stack) {
            Stack stack = (Stack)resource;
            if (!ServiceConstants.isSystem(stack)) {
                Account account = objectManager.loadResource(Account.class, stack.getAccountId());
                if (DataAccessor.fieldString(account, AccountConstants.FIELD_ORCHESTRATION) != null) {
                    return;
                }
            }
            trigger(stack.getAccountId());
        } else if (resource instanceof Host) {
            trigger(((Host) resource).getAccountId());
        }
    }

    public void trigger(Long accountId) {
        loopManager.kick(LoopFactory.SYSTEM_STACK, Account.class, accountId, null);
    }

}
