package io.cattle.platform.process.account;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.api.pubsub.manager.SubscribeManager;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.ServicesPortRange;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.UserPreference;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.cattle.platform.core.model.Tables.*;
import static io.cattle.platform.core.model.tables.CredentialTable.CREDENTIAL;
import static io.cattle.platform.core.model.tables.NetworkTable.NETWORK;

public class AccountProcessManager {

    public static final DynamicBooleanProperty CREATE_CREDENTIAL = ArchaiusUtil.getBoolean("process.account.create.create.credential");
    public static final DynamicStringProperty CREDENTIAL_TYPE = ArchaiusUtil.getString("process.account.create.create.credential.default.kind");
    public static final DynamicStringListProperty ACCOUNT_KIND_CREDENTIALS = ArchaiusUtil.getList("process.account.create.create.credential.account.kinds");

    private static final Class<?>[] REMOVE_TYPES = new Class<?>[]{
        Service.class,
        Stack.class,
        Agent.class,
        Certificate.class,
        Credential.class,
        Volume.class,
        GenericObject.class,
        UserPreference.class,
        Instance.class,
    };

    NetworkDao networkDao;
    GenericResourceDao resourceDao;
    ObjectProcessManager processManager;
    ObjectManager objectManager;
    InstanceDao instanceDao;
    AccountDao accountDao;
    ServiceDao serviceDao;
    EventService eventService;

    public AccountProcessManager(NetworkDao networkDao, GenericResourceDao resourceDao, ObjectProcessManager processManager, ObjectManager objectManager,
            InstanceDao instanceDao, AccountDao accountDao, ServiceDao serviceDao, EventService eventService) {
        super();
        this.networkDao = networkDao;
        this.resourceDao = resourceDao;
        this.processManager = processManager;
        this.objectManager = objectManager;
        this.instanceDao = instanceDao;
        this.accountDao = accountDao;
        this.serviceDao = serviceDao;
        this.eventService = eventService;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();
        boolean createApiKey = DataAccessor
                .fromMap(state.getData())
                .withScope(AccountConstants.class)
                .withKey(AccountConstants.OPTION_CREATE_APIKEY)
                .withDefault(false)
                .as(Boolean.class);

        String apiKeyKind = DataAccessor.fromMap(state.getData())
                .withScope(AccountConstants.class)
                .withKey(AccountConstants.OPTION_CREATE_APIKEY_KIND)
                .withDefault(CREDENTIAL_TYPE.get())
                .as(String.class);

        if (shouldCreateCredentials(account, state)) {
            if (createApiKey || CREATE_CREDENTIAL.get()) {
                List<Credential> creds = objectManager.children(account, Credential.class);
                if (creds.size() == 0) {
                    creds = Collections.singletonList(objectManager.create(Credential.class,
                                CREDENTIAL.ACCOUNT_ID, account.getId(),
                                CREDENTIAL.KIND, apiKeyKind));
                }

                for (Credential cred : creds) {
                    processManager.executeCreateThenActivate(cred, null);
                }
            }
        }

        createOwnerAccess(state, account);
        createDefaultStack(account);
        assignExternalId(account);

        return setupNetworking(account);
    }

    private void assignExternalId(Account account) {
        String id = account.getExternalId();
        if (StringUtils.isBlank(id)) {
            account.setExternalId((account.getName() + account.getUuid().substring(0,8)).toLowerCase());
        }
    }

    public HandlerResult update(ProcessState state, ProcessInstance process) {
        Account account = (Account)state.getResource();

        createDefaultStack(account);
        disconnectClients(account);

        return null;
    }

    private void createDefaultStack(Account account) {
        if (account.getClusterId() != null) {
            serviceDao.getOrCreateDefaultStack(account.getId());
        }
    }

    private void disconnectClients(Account account) {
        /* Since the clusterId may have changed, we disconnect all clients because the cluster ID may have been
         * cached in the connection as null
         */
        disconnectClients(this.eventService, account);
    }

    public static void disconnectClients(EventService eventService, Account account) {
        String event = FrameworkEvents.appendAccount(SubscribeManager.EVENT_DISCONNECT, account.getId());
        eventService.publish(EventVO.newEvent(event));
    }

    private void createOwnerAccess(ProcessState state, Account account) {
        Boolean createOwnerAccess = (DataAccessor.fromDataFieldOf(state)
                .withKey(AccountConstants.OPTION_CREATE_OWNER_ACCESS)
                .as(Boolean.class));

        if (createOwnerAccess == null || !createOwnerAccess) {
            return;
        }

        Account owner = objectManager.findAny(Account.class,
                ACCOUNT.CLUSTER_ID, account.getClusterId(),
                ACCOUNT.CLUSTER_OWNER, true,
                ACCOUNT.REMOVED, null);
        if (owner == null) {
            return;
        }

        Set<String> existingAccess = new HashSet<>();
        for (ProjectMember member : objectManager.find(ProjectMember.class,
                PROJECT_MEMBER.PROJECT_ID, account.getId())) {
            existingAccess.add(externalId(member));
        }

        for (ProjectMember member : objectManager.find(ProjectMember.class,
                PROJECT_MEMBER.STATE, CommonStatesConstants.ACTIVE,
                PROJECT_MEMBER.ROLE, ProjectConstants.OWNER,
                PROJECT_MEMBER.PROJECT_ID, owner.getId())) {
            if (existingAccess.contains(externalId(member))) {
                continue;
            }

            objectManager.create(ProjectMember.class,
                    PROJECT_MEMBER.ACCOUNT_ID, account.getId(),
                    PROJECT_MEMBER.PROJECT_ID, account.getId(),
                    PROJECT_MEMBER.EXTERNAL_ID, member.getExternalId(),
                    PROJECT_MEMBER.EXTERNAL_ID_TYPE, member.getExternalIdType(),
                    PROJECT_MEMBER.STATE, CommonStatesConstants.ACTIVE,
                    PROJECT_MEMBER.ROLE, ProjectConstants.OWNER);
        }
    }

    private String externalId(ProjectMember member) {
        return String.format("%s:%s", member.getExternalIdType(), member.getExternalId());
    }

    public HandlerResult remove(ProcessState state, ProcessInstance process) {
        Account account = (Account)state.getResource();

        DeferredUtils.defer(() -> disconnectClients(account));

        // For agent accounts do purge logic in remove.
        if (AccountConstants.AGENT_KIND.equals(account.getKind()) ||
                AccountConstants.REGISTERED_AGENT_KIND.equals(account.getKind())) {
            purge(state, process);
        }

        return null;
    }

    public HandlerResult purge(ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();

        for (Class<?> clz : REMOVE_TYPES) {
            for (Object obj : list(account, clz)) {
                if (obj instanceof Instance) {
                    Instance instance = (Instance)obj;
                    deleteAgentAccount(instance.getAgentId(), state.getData());
                    processManager.stopThenRemove(instance, null);
                } else {
                    processManager.deactivateThenRemove(obj, null);
                }
            }
        }

        accountDao.deleteProjectMemberEntries(account);

        List<Agent> resourceAgents = objectManager.find(Agent.class,
                AGENT.REMOVED, null,
                AGENT.RESOURCE_ACCOUNT_ID, account.getId());

        for (Agent agent : resourceAgents) {
            Account agentAccount = objectManager.loadResource(Account.class, agent.getAccountId());
            processManager.deactivateThenRemove(agentAccount, null);
        }

        return null;
    }

    private HandlerResult setupNetworking(Account account) {
        if (!account.getClusterOwner() || account.getClusterId() == null) {
            return null;
        }

        Map<String, Network> networksByKind = getNetworksByKind(account);

        createNetwork(NetworkConstants.KIND_DOCKER_HOST, account, networksByKind, "Host Network Mode");
        createNetwork(NetworkConstants.KIND_DOCKER_NONE, account, networksByKind, "No Network Mode");
        createNetwork(NetworkConstants.KIND_DOCKER_CONTAINER, account, networksByKind, "Container Network Mode");
        createNetwork(NetworkConstants.KIND_DOCKER_BRIDGE, account, networksByKind, "Docker Bridge Network Mode");

        ServicesPortRange portRange = DataAccessor.field(account, AccountConstants.FIELD_PORT_RANGE, ServicesPortRange.class);
        if (portRange == null) {
            portRange = AccountConstants.getDefaultServicesPortRange();
        }

        return new HandlerResult(AccountConstants.FIELD_PORT_RANGE, portRange);
    }

    protected void createNetwork(String kind, Account account, Map<String, Network> networksByKind, String name) {
        Network network = networksByKind.get(kind);
        if (network != null) {
            return;
        }

        resourceDao.createAndSchedule(Network.class,
                ObjectMetaDataManager.NAME_FIELD, name,
                ObjectMetaDataManager.NAME_FIELD, name,
                NETWORK.CLUSTER_ID, account.getClusterId(),
                ObjectMetaDataManager.KIND_FIELD, kind);
    }


    protected Map<String, Network> getNetworksByKind(Account account) {
        Map<String, Network> result = new HashMap<>();

        for (Network network : objectManager.find(Network.class,
                NETWORK.CLUSTER_ID, account.getClusterId(),
                NETWORK.REMOVED, null)) {
            result.put(network.getKind(), network);
        }

        return result;
    }



    private boolean shouldCreateCredentials(Account account, ProcessState state) {
        return ACCOUNT_KIND_CREDENTIALS.get().contains(account.getKind());
    }

    protected <T> List<T> list(Account account, Class<T> type) {
        return objectManager.find(type,
                ObjectMetaDataManager.REMOVED_FIELD, null,
                ObjectMetaDataManager.ACCOUNT_FIELD, account.getId());
    }

    protected void deleteAgentAccount(Long agentId, Map<String, Object> data) {
        if (agentId == null) {
            return;
        }

        Agent agent = objectManager.loadResource(Agent.class, agentId);
        Account account  = objectManager.loadResource(Account.class, agent.getAccountId());
        if (account == null) {
            return;
        }

        processManager.executeDeactivateThenScheduleRemove(account, data);
    }

}
