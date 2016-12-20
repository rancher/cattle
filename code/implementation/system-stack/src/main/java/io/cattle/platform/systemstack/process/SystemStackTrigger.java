package io.cattle.platform.systemstack.process;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SystemStackTrigger extends AbstractObjectProcessLogic implements ProcessPostListener, ProcessPreListener {
    public static final String STACKS = "system-stacks";

    @Inject
    ConfigItemStatusManager itemManager;

    @Override
    public String[] getProcessNames() {
        return new String[]{
            "stack.*", "host.*"
        };
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
