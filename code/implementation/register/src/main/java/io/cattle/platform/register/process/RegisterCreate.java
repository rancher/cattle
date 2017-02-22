package io.cattle.platform.register.process;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractGenericObjectProcessLogic;
import io.cattle.platform.register.dao.RegisterDao;
import io.cattle.platform.register.util.RegisterConstants;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class RegisterCreate extends AbstractGenericObjectProcessLogic implements ProcessHandler {

    RegisterDao registerDao;
    ResourceMonitor resourceMonitor;

    @Override
    public String[] getProcessNames() {
        return new String[] { "genericobject.create" };
    }

    @Override
    public String getKind() {
        return "register";
    }

    @Override
    protected HandlerResult handleKind(final ProcessState state, ProcessInstance process) {
        GenericObject resource = (GenericObject) state.getResource();
        String key = resource.getKey();

        if (key == null) {
            return null;
        }

        Agent agent;
        Long agentId = DataAccessor.fromDataFieldOf(resource).withKey(RegisterConstants.DATA_AGENT_ID).as(Long.class);

        agent = loadResource(Agent.class, agentId);

        if (agent == null) {
            agent = registerDao.createAgentForRegistration(key, resource);
        }

        final Agent agentFinal = agent;
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                try {
                    objectProcessManager.scheduleStandardProcess(StandardProcess.CREATE, agentFinal, state.getData());
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

    protected Credential getCredential(Agent agent) {
        Account account = loadResource(Account.class, agent.getAccountId());

        for (Credential cred : children(account, Credential.class)) {
            if (cred.getKind().equals(CredentialConstants.KIND_AGENT_API_KEY) && CommonStatesConstants.ACTIVE.equals(cred.getState())) {
                return cred;
            }
        }

        return null;
    }

    public RegisterDao getRegisterDao() {
        return registerDao;
    }

    @Inject
    public void setRegisterDao(RegisterDao registerDao) {
        this.registerDao = registerDao;
    }

    public ResourceMonitor getResourceMonitor() {
        return resourceMonitor;
    }

    @Inject
    public void setResourceMonitor(ResourceMonitor resourceMonitor) {
        this.resourceMonitor = resourceMonitor;
    }

}
