package io.cattle.platform.process.account;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AccountRemove extends AbstractDefaultProcessHandler {

    @Inject
    AccountPurge accountPurge;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account account = (Account)state.getResource();

        // For agent accounts do purge logic in remove.
        if (AccountConstants.AGENT_KIND.equals(account.getKind()) ||
                AccountConstants.REGISTERED_AGENT_KIND.equals(account.getKind())) {
            accountPurge.handle(state, process);
        }

        return null;
    }

}
