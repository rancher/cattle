package io.cattle.platform.register.process;

import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.GenericObjectTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.RegisterConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.register.dao.RegisterDao;

import com.netflix.config.DynamicStringListProperty;

public class RegisterProcessManager {

    public static final DynamicStringListProperty ACCOUNT_KINDS = ArchaiusUtil.getList("process.account.create.register.token.account.kinds");

    RegisterDao registerDao;
    ResourceMonitor resourceMonitor;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    GenericResourceDao resourceDao;

    public RegisterProcessManager(RegisterDao registerDao, ResourceMonitor resourceMonitor, ObjectManager objectManager, ObjectProcessManager processManager,
            GenericResourceDao resourceDao) {
        super();
        this.registerDao = registerDao;
        this.resourceMonitor = resourceMonitor;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.resourceDao = resourceDao;
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

        Agent agent;
        Long agentId = DataAccessor.fromDataFieldOf(resource).withKey(RegisterConstants.DATA_AGENT_ID).as(Long.class);

        agent = objectManager.loadResource(Agent.class, agentId);

        if (agent == null) {
            agent = registerDao.createAgentForRegistration(key, resource);
        }

        final Agent agentFinal = agent;
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                try {
                    processManager.createThenActivate(agentFinal, state.getData());
                } catch (ProcessCancelException e) {
                }
            }
        });

        agent = resourceMonitor.waitFor(agent, new ResourcePredicate<Agent>() {
            @Override
            public boolean evaluate(Agent obj) {
                return getCredential(obj) != null;
            }

            @Override
            public String getMessage() {
                return "credentials assigned";
            }
        });

        Credential cred = getCredential(agent);

        return new HandlerResult(RegisterConstants.FIELD_ACCESS_KEY, cred.getPublicValue(), RegisterConstants.FIELD_SECRET_KEY, cred.getSecretValue());
    }

    public HandlerResult accountCreate(final ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();

        if (!ACCOUNT_KINDS.get().contains(account.getKind())) {
            return null;
        }

        boolean found = false;
        for (Credential cred : objectManager.children(account, Credential.class)) {
            if (CredentialConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN.equals(cred.getKind())) {
                found = true;
                break;
            }
        }

        if (!found) {
            resourceDao.createAndSchedule(Credential.class,
                    CREDENTIAL.ACCOUNT_ID, account.getId(),
                    CREDENTIAL.KIND, CredentialConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN);
        }

        return null;
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
