package io.github.ibuildthecloud.dstack.process.agent;

import static io.github.ibuildthecloud.dstack.core.model.tables.AccountTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.AgentTable.*;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;
import io.github.ibuildthecloud.dstack.process.dao.AccountDao;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicBooleanProperty;

@Named
public class AgentCreate extends AbstractDefaultProcessHandler {

    private static final DynamicBooleanProperty CREATE_ACCOUNT = ArchaiusUtil.getBoolean("process.agent.create.create.account");

    AccountDao accountDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent)state.getResource();
        Long accountId = agent.getAccountId();

        if ( accountId != null || ! CREATE_ACCOUNT.get() ) {
            return new HandlerResult();
        }

        Account account = createAccountObj(agent);
        getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, account, null);

        return new HandlerResult(AGENT.ACCOUNT_ID, account.getId());
    }

    protected Account createAccountObj(Agent agent) {
        if ( agent.getAccountId() != null ) {
            return getObjectManager().loadResource(Account.class, agent.getAccountId());
        }

        String uuid = getAccountUuid(agent);
        Account account = accountDao.findByUuid(uuid);

        if ( account == null ) {
            account = getObjectManager().create(Account.class,
                    ACCOUNT.UUID, uuid,
                    ACCOUNT.KIND, "agent");
        }

        return account;
    }

    protected String getAccountUuid(Agent agent) {
        return "agentAccount" + agent.getId();
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    @Inject
    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

}
