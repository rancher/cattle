package io.cattle.platform.process.register;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.RegisterConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.RegisterDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;

import static io.cattle.platform.core.model.tables.GenericObjectTable.*;

public class RegisterProcessManager {

    AccountDao accountDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    RegisterDao registerDao;
    ResourceMonitor resourceMonitor;

    public RegisterProcessManager(RegisterDao registerDao, ResourceMonitor resourceMonitor, ObjectManager objectManager, ObjectProcessManager processManager,
            AccountDao accountDao) {
        super();
        this.registerDao = registerDao;
        this.resourceMonitor = resourceMonitor;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.accountDao = accountDao;
    }

    public HandlerResult genericObjectCreate(final ProcessState state, ProcessInstance process) {
        GenericObject resource = (GenericObject) state.getResource();
        if (!"register".equals(resource.getKind())) {
            return null;
        }

        String key = resource.getKey();
        if (key == null) {
            return null;
        }

        if (resource.getClusterId() == null) {
            return null;
        }

        long clusterAccountId = accountDao.getAccountIdForCluster(resource.getClusterId());

        Agent agent;
        Long agentId = DataAccessor.fromDataFieldOf(resource).withKey(RegisterConstants.DATA_AGENT_ID).as(Long.class);

        agent = objectManager.loadResource(Agent.class, agentId);

        if (agent == null) {
            agent = registerDao.createAgentForRegistration(key, clusterAccountId, resource.getClusterId(), resource);
        }

        final Agent agentFinal = agent;
        if (!CommonStatesConstants.ACTIVE.equalsIgnoreCase(agentFinal.getState())) {
            DeferredUtils.nest(() -> processManager.createThenActivate(agentFinal, state.getData()));
        }

        Credential cred = getCredential(agent);
        if (cred == null) {
            return new HandlerResult().withFuture(
                    resourceMonitor.waitFor(agent, "credentials assigned", (checkAgent) -> getCredential(checkAgent) != null));
        }

        return new HandlerResult(
                RegisterConstants.FIELD_ACCESS_KEY, cred.getPublicValue(),
                RegisterConstants.FIELD_SECRET_KEY, cred.getSecretValue());
    }

    public HandlerResult assignCredentials(ProcessState state, ProcessInstance process) {
        return genericObjectCreate(state, process);
    }

    public HandlerResult agentRemove(ProcessState state, ProcessInstance process) {
        String key = DataAccessor.fromDataFieldOf(state.getResource())
                .withKey(RegisterConstants.AGENT_DATA_REGISTRATION_KEY).as(String.class);

        if (key != null) {
            for (GenericObject obj : objectManager.find(GenericObject.class,
                    GENERIC_OBJECT.KEY, key,
                    GENERIC_OBJECT.REMOVED, null)) {
                processManager.executeDeactivateThenScheduleRemove(obj, state.getData());
            }
        }

        return null;
    }

    protected Credential getCredential(Agent agent) {
        Account account = objectManager.loadResource(Account.class, agent.getAccountId());

        for (Credential cred : objectManager.children(account, Credential.class)) {
            if (cred.getKind().equals(CredentialConstants.KIND_AGENT_API_KEY) && CommonStatesConstants.ACTIVE.equals(cred.getState())) {
                return cred;
            }
        }

        return null;
    }

}
