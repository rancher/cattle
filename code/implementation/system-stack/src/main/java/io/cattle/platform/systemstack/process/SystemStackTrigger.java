package io.cattle.platform.systemstack.process;

import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;

public class SystemStackTrigger extends AbstractObjectProcessLogic implements ProcessPostListener {
    public static final String STACKS = "system-stacks";

    @Inject
    ConfigItemStatusManager itemManager;

    @Override
    public String[] getProcessNames() {
        return new String[]{
            "host.activate",
            "account.update",
        };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        Object accountId = ObjectUtils.getAccountId(resource);
        if (resource instanceof Account) {
            accountId = ((Account) resource).getId();
        }

        if (!(accountId instanceof Number)) {
            return null;
        }

        ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Account.class, ((Number)accountId).longValue());
        request.addItem(STACKS);
        request.withDeferredTrigger(true);
        itemManager.updateConfig(request);
        return null;
    }

}