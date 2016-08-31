package io.cattle.platform.process.account;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AccountPurge extends AbstractDefaultProcessHandler {

    @Inject
    InstanceDao instanceDao;
    @Inject
    AccountDao accountDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();

        for (Certificate cert : getObjectManager().children(account, Certificate.class)) {
            if (cert.getRemoved() != null) {
                continue;
            }

            deactivateThenRemove(cert, state.getData());
        }

        for (Credential cred : getObjectManager().children(account, Credential.class)) {
            if (cred.getRemoved() != null) {
                continue;
            }

            deactivateThenRemove(cred, state.getData());
        }

        for (Stack env : getObjectManager().children(account, Stack.class)) {
            if (env.getRemoved() != null) {
                continue;
            }
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, env, null);
        }

        for (Instance instance : instanceDao.listNonRemovedInstances(account, false)) {
            deleteAgentAccount(instance.getAgentId(), state.getData());

            try {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, instance, null);
            } catch (ProcessCancelException e) {
                objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance,
                        CollectionUtils.asMap(InstanceConstants.REMOVE_OPTION, true));
            }
        }

        for (Host host : getObjectManager().children(account, Host.class)) {
            try {
                deactivateThenRemove(host, state.getData());
            } catch (ProcessCancelException e) {
                // ignore
            }
            purge(host, null);
        }

        for (PhysicalHost host : getObjectManager().children(account, PhysicalHost.class)) {
            try {
                getObjectProcessManager().executeStandardProcess(StandardProcess.REMOVE, host, null);
            } catch (ProcessCancelException e) {
                // ignore
            }
        }

        for (Agent agent : getObjectManager().children(account, Agent.class)) {
            if (agent.getRemoved() != null) {
                continue;
            }
            deactivateThenRemove(agent, state.getData());
        }

        accountDao.deleteProjectMemberEntries(account);

        return null;
    }

    protected void deleteAgentAccount(Long agentId, Map<String, Object> data) {
        if (agentId == null) {
            return;
        }

        Agent agent = getObjectManager().loadResource(Agent.class, agentId);
        Account account  = getObjectManager().loadResource(Account.class, agent.getAccountId());
        if (account == null) {
            return;
        }

        deactivateThenScheduleRemove(account, data);
    }

}
