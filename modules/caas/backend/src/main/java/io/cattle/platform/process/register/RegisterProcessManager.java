package io.cattle.platform.process.register;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.addon.K8sClientConfig;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.GenericObjectConstants;
import io.cattle.platform.core.constants.RegisterConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.dao.RegisterDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.account.AccountProcessManager;
import io.cattle.platform.process.lock.AccountClusterCreateLock;

import java.util.function.Function;

import static io.cattle.platform.core.model.tables.GenericObjectTable.*;

public class RegisterProcessManager {

    AccountDao accountDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    RegisterDao registerDao;
    ResourceMonitor resourceMonitor;
    EventService eventService;
    LockManager lockManager;
    ClusterDao clusterDao;

    public RegisterProcessManager(RegisterDao registerDao, ResourceMonitor resourceMonitor, ObjectManager objectManager, ObjectProcessManager processManager,
            AccountDao accountDao, EventService eventService, LockManager lockManager, ClusterDao clusterDao) {
        super();
        this.registerDao = registerDao;
        this.resourceMonitor = resourceMonitor;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.accountDao = accountDao;
        this.eventService = eventService;
        this.lockManager = lockManager;
        this.clusterDao = clusterDao;
    }

    public HandlerResult genericObjectPreCreate(ProcessState state, ProcessInstance process) {
        return forRegister(state, this::createCluster);
    }

    public HandlerResult genericObjectCreate(ProcessState state, ProcessInstance process) {
        return forRegister(state, this::createAgent);
    }

    public HandlerResult genericObjectPostCreate(ProcessState state, ProcessInstance process) {
        // We run this a second time because the first call may have been async and not recorded the
        // credential keys yet.  Calling a second time till do that.
        return genericObjectCreate(state, process);
    }

    private HandlerResult createCluster(GenericObject resource) {
        if (resource.getClusterId() != null) {
            // Cluster already exists
            return null;
        }

        return lockManager.lock(new AccountClusterCreateLock(resource.getAccountId()),
                () -> createClusterWithLock(resource));
    }

    private HandlerResult createClusterWithLock(GenericObject resource) {
        Account account = objectManager.loadResource(Account.class, resource.getAccountId());
        if (account.getClusterId() != null) {
            // Cluster already created
            return new HandlerResult(
                    GENERIC_OBJECT.CLUSTER_ID, resource.getClusterId(),
                    GenericObjectConstants.FIELD_CLUSTER_CREATOR, DataAccessor.fieldBool(resource, GenericObjectConstants.FIELD_CLUSTER_CREATOR));
        }

        K8sClientConfig clientConfig = null;

        // If k8s config is not empty then we are importing a cluster and at this time just creating the cluster, not a host
        // FYI, when registering a custom host there is the situation in which we both create the cluster and a host agent
        boolean importCluster = DataAccessor.fieldMapRO(resource, GenericObjectConstants.FIELD_K8S_CLIENT_CONFIG).size() > 0;
        if (importCluster) {
            clientConfig = DataAccessor.field(resource, GenericObjectConstants.FIELD_K8S_CLIENT_CONFIG, K8sClientConfig.class);
        }

        Cluster cluster = clusterDao.createClusterForAccount(account, clientConfig);

        ListenableFuture<?> future = AsyncUtils.andThen(resourceMonitor.waitForState(cluster, CommonStatesConstants.ACTIVE), (ignore) -> {
            AccountProcessManager.disconnectClients(eventService, account);
            return ignore;
        });

        return new HandlerResult(
                GENERIC_OBJECT.CLUSTER_ID, cluster.getId(),
                GenericObjectConstants.FIELD_CLUSTER_CREATOR, importCluster)
                .withFuture(future);
    }

    private HandlerResult createAgent(GenericObject register) {
        if (register.getClusterId() == null) {
            // Need a cluster for an agent
            return null;
        }

        if (DataAccessor.fieldBool(register, GenericObjectConstants.FIELD_CLUSTER_CREATOR)) {
            // This register object is used to create a cluster only, not hosts, no agent should be created
            return null;
        }

        Long agentId = DataAccessor.fieldLong(register, GenericObjectConstants.FIELD_AGENT_ID);
        Agent agent = agentId == null ?
                registerDao.createAgentForRegistration(register.getKey(), register.getClusterId(), register) :
                objectManager.loadResource(Agent.class, agentId);
        if (agent == null) {
            return null;
        }

        if (!CommonStatesConstants.ACTIVE.equalsIgnoreCase(agent.getState())) {
            DeferredUtils.nest(() -> processManager.createThenActivate(agent, null));
        }

        Credential cred = getCredential(agent);
        if (cred == null) {
            return new HandlerResult(resourceMonitor.waitFor(agent, "credentials assigned",
                    (checkAgent) -> getCredential(checkAgent) != null));
        }

        return new HandlerResult(
                RegisterConstants.FIELD_ACCESS_KEY, cred.getPublicValue(),
                RegisterConstants.FIELD_SECRET_KEY, cred.getSecretValue());
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

    private HandlerResult forRegister(ProcessState state, Function<GenericObject, HandlerResult> func) {
        GenericObject resource = (GenericObject) state.getResource();
        if (!GenericObjectConstants.KIND_REGISTER.equals(resource.getKind())) {
            return null;
        }

        String key = resource.getKey();
        if (key == null) {
            return null;
        }

        return func.apply(resource);
    }

}
