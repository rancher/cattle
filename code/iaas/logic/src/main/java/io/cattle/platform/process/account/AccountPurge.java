package io.cattle.platform.process.account;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.Map;
import javax.inject.Named;

@Named
public class AccountPurge extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();

        for (Credential cred : getObjectManager().children(account, Credential.class)) {
            if (cred.getRemoved() != null) {
                continue;
            }

            deactivateThenRemove(cred, state.getData());
        }

        for (Host host : getObjectManager().children(account, Host.class)) {
            try {
                deactivateThenRemove(host, state.getData());
            } catch (ProcessCancelException e) {
                // ignore
            }
            purge(host, null);

            deleteAgentAccount(host.getAgentId(), state.getData());
        }

        for (Agent agent : getObjectManager().children(account, Agent.class)) {
            if (agent.getRemoved() != null) {
                continue;
            }
            deactivateThenRemove(agent, state.getData());
        }

        for (Instance instance : getObjectManager().children(account, Instance.class)) {
            deleteAgentAccount(instance.getAgentId(), state.getData());

            if (instance.getRemoved() != null) {
                continue;
            }

            try {
                objectProcessManager.executeProcess(InstanceConstants.PROCESS_STOP, instance, null);
            } catch (ProcessCancelException e) {
                // ignore
            }

            remove(instance, null);
        }

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

        try {
            deactivateThenRemove(account, data);
        } catch (ProcessCancelException e) {
            // ignore
        }

        purge(account, data);
    }

}
