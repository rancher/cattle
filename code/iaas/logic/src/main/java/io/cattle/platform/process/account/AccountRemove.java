package io.cattle.platform.process.account;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AccountRemove extends AbstractDefaultProcessHandler {

    @Inject
    AccountPurge accountPurge;
    @Inject
    AccountDao accountDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account account = (Account)state.getResource();

        // For agent accounts do purge logic in remove.
        if (AccountConstants.AGENT_KIND.equals(account.getKind()) ||
                AccountConstants.REGISTERED_AGENT_KIND.equals(account.getKind())) {
            accountPurge.handle(state, process);
        }

        accountDao.generateAccountLinks(account, new ArrayList<Long>());

        List<? extends AccountLink> refsBy = objectManager.find(AccountLink.class,
                ACCOUNT_LINK.LINKED_ACCOUNT_ID, account.getId(),
                ACCOUNT_LINK.REMOVED, null);
        for (AccountLink refBy : refsBy) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, refBy, null);
        }

        return null;
    }

}
