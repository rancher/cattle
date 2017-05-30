package io.cattle.platform.process.account;

import static io.cattle.platform.core.model.tables.HostTable.*;

import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.UserPreference;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.List;
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

        for (Stack env : list(account, Stack.class)) {
            if (env.getRemoved() != null) {
                continue;
            }
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, env, null);
        }

        for (Agent agent : list(account, Agent.class)) {
            if (agent.getRemoved() != null) {
                continue;
            }
            deactivateThenRemove(agent, state.getData());
        }

        for (Host host : hostList(account, Host.class)) {
            try {
                deactivateThenRemove(host, state.getData());
            } catch (ProcessCancelException e) {
                // ignore
            }
        }

        for (Certificate cert : list(account, Certificate.class)) {
            if (cert.getRemoved() != null) {
                continue;
            }

            deactivateThenRemove(cert, state.getData());
        }

        for (Credential cred : list(account, Credential.class)) {
            if (cred.getRemoved() != null) {
                continue;
            }

            deactivateThenRemove(cred, state.getData());
        }

        for (Instance instance : instanceDao.listNonRemovedNonStackInstances(account)) {
            deleteAgentAccount(instance.getAgentId(), state.getData());
            objectProcessManager.stopAndRemove(instance, null);
        }

        for (PhysicalHost host : list(account, PhysicalHost.class)) {
            try {
                getObjectProcessManager().executeStandardProcess(StandardProcess.REMOVE, host, null);
            } catch (ProcessCancelException e) {
                // ignore
            }
        }

        for (StoragePool pool : list(account, StoragePool.class)) {
            if (pool.getRemoved() != null) {
                continue;
            }

            deactivateThenRemove(pool, state.getData());
        }

        for (Volume volume : list(account, Volume.class)) {
            if (volume.getRemoved() != null) {
                continue;
            }

            deactivateThenRemove(volume, state.getData());
        }

        for (Network network : list(account, Network.class)) {
            if (network.getRemoved() != null) {
                continue;
            }

            deactivateThenRemove(network, state.getData());
        }

        for (GenericObject gobject : list(account, GenericObject.class)) {
            if (gobject.getRemoved() != null) {
                continue;
            }

            deactivateThenRemove(gobject, state.getData());
        }

        for (UserPreference userPreference : list(account, UserPreference.class)) {
            if (userPreference.getRemoved() != null) {
                continue;
            }

            deactivateThenRemove(userPreference, state.getData());
        }

        accountDao.deleteProjectMemberEntries(account);

        return null;
    }

    protected <T> List<T> list(Account account, Class<T> type) {
        return objectManager.find(type,
                ObjectMetaDataManager.REMOVED_FIELD, null,
                ObjectMetaDataManager.ACCOUNT_FIELD, account.getId());
    }

    protected <T> List<T> hostList(Account account, Class<T> type) {
        return objectManager.find(type,
                ObjectMetaDataManager.REMOVED_FIELD, null,
                ObjectMetaDataManager.ACCOUNT_FIELD, account.getId(), HOST.STACK_ID, null);
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
