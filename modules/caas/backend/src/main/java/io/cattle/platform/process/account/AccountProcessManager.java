package io.cattle.platform.process.account;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;
import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.ProjectTemplateTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.ServicesPortRange;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.UserPreference;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;

public class AccountProcessManager {

    public static final DynamicBooleanProperty CREATE_CREDENTIAL = ArchaiusUtil.getBoolean("process.account.create.create.credential");
    public static final DynamicStringProperty CREDENTIAL_TYPE = ArchaiusUtil.getString("process.account.create.create.credential.default.kind");
    public static final DynamicStringListProperty ACCOUNT_KIND_CREDENTIALS = ArchaiusUtil.getList("process.account.create.create.credential.account.kinds");
    public static final DynamicStringProperty DEFAULT_TEMPLATE = ArchaiusUtil.getString("project.template.default.name");
    public static final DynamicStringListProperty KINDS = ArchaiusUtil.getList("docker.network.create.account.types");

    private static final Class<?>[] REMOVE_TYPES = new Class<?>[]{
        Stack.class,
        Agent.class,
        Host.class,
        Certificate.class,
        Credential.class,
        PhysicalHost.class,
        StoragePool.class,
        Volume.class,
        Network.class,
        GenericObject.class,
        UserPreference.class
    };

    NetworkDao networkDao;
    GenericResourceDao resourceDao;
    ObjectProcessManager processManager;
    ObjectManager objectManager;
    InstanceDao instanceDao;
    AccountDao accountDao;

    public AccountProcessManager(NetworkDao networkDao, GenericResourceDao resourceDao, ObjectProcessManager processManager, ObjectManager objectManager,
            InstanceDao instanceDao, AccountDao accountDao) {
        super();
        this.networkDao = networkDao;
        this.resourceDao = resourceDao;
        this.processManager = processManager;
        this.objectManager = objectManager;
        this.instanceDao = instanceDao;
        this.accountDao = accountDao;
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
                    creds = Arrays.asList(objectManager.create(Credential.class,
                            CREDENTIAL.ACCOUNT_ID, account.getId(),
                            CREDENTIAL.KIND, apiKeyKind));
                }

                for (Credential cred : creds) {
                    processManager.executeCreateThenActivate(cred, null);
                }
            }
        }

        List<? extends Long> accountLinks = DataAccessor.fromMap(state.getData()).withKey(
                AccountConstants.FIELD_ACCOUNT_LINKS).withDefault(Collections.EMPTY_LIST)
            .asList(Long.class);

        accountDao.generateAccountLinks(account, accountLinks);

        return setupNetworking(account);
    }

    public HandlerResult preCreate(ProcessState state, ProcessInstance process) {
        Account account = (Account)state.getResource();
        if (account.getProjectTemplateId() != null || !AccountConstants.PROJECT_KIND.equals(account.getKind())) {
            return null;
        }

        if (StringUtils.isBlank(DEFAULT_TEMPLATE.get())) {
            return null;
        }

        ProjectTemplate template = objectManager.findAny(ProjectTemplate.class,
                PROJECT_TEMPLATE.NAME, DEFAULT_TEMPLATE.get(),
                PROJECT_TEMPLATE.IS_PUBLIC, true,
                PROJECT_TEMPLATE.REMOVED, null);

        return new HandlerResult(
                ACCOUNT.PROJECT_TEMPLATE_ID, template == null ? null : template.getId());
    }

    public HandlerResult remove(ProcessState state, ProcessInstance process) {
        Account account = (Account)state.getResource();

        // For agent accounts do purge logic in remove.
        if (AccountConstants.AGENT_KIND.equals(account.getKind()) ||
                AccountConstants.REGISTERED_AGENT_KIND.equals(account.getKind())) {
            purge(state, process);
        }

        accountDao.generateAccountLinks(account, new ArrayList<Long>());

        List<? extends AccountLink> refsBy = objectManager.find(AccountLink.class,
                ACCOUNT_LINK.LINKED_ACCOUNT_ID, account.getId(),
                ACCOUNT_LINK.REMOVED, null);
        for (AccountLink refBy : refsBy) {
            processManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, refBy, null);
        }

        return null;
    }

    public HandlerResult purge(ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();

        for (Class<?> clz : REMOVE_TYPES) {
            for (Object obj : list(account, clz)) {
                processManager.executeDeactivateThenRemove(obj, null);
            }
        }

        for (Instance instance : instanceDao.listNonRemovedNonStackInstances(account)) {
            deleteAgentAccount(instance.getAgentId(), state.getData());
            processManager.stopThenRemove(instance, null);
        }

        accountDao.deleteProjectMemberEntries(account);
        return null;
    }

    public HandlerResult update(ProcessState state, ProcessInstance process) {
        Account account = (Account) state.getResource();
        List<? extends Long> accountLinks = DataAccessor
                .fromMap(state.getData())
                .withKey(AccountConstants.FIELD_ACCOUNT_LINKS)
                .withDefault(Collections.EMPTY_LIST)
                .asList(Long.class);

        accountDao.generateAccountLinks(account, accountLinks);
        return null;
    }

    private HandlerResult setupNetworking(Account account) {
        if (!KINDS.get().contains(account.getKind())) {
            return null;
        }

        Map<String, Network> networksByKind = getNetworksByKind(account);

        createNetwork(NetworkConstants.KIND_DOCKER_HOST, account, networksByKind, "Docker Host Network Mode", null);
        createNetwork(NetworkConstants.KIND_DOCKER_NONE, account, networksByKind, "Docker None Network Mode", null);
        createNetwork(NetworkConstants.KIND_DOCKER_CONTAINER, account, networksByKind, "Docker Container Network Mode", null);
        createNetwork(NetworkConstants.KIND_DOCKER_BRIDGE, account, networksByKind, "Docker Bridge Network Mode", null);

        ServicesPortRange portRange = DataAccessor.field(account, AccountConstants.FIELD_PORT_RANGE, ServicesPortRange.class);
        if (portRange == null) {
            portRange = AccountConstants.getDefaultServicesPortRange();
        }

        return new HandlerResult(AccountConstants.FIELD_PORT_RANGE, portRange);
    }

    protected Network createNetwork(String kind, Account account, Map<String, Network> networksByKind,
                                  String name, String key, Object... valueKeyValue) {
        Network network = networksByKind.get(kind);
        if (network != null) {
            return network;
        }
        Map<String, Object> data = key == null ? new HashMap<>() :
                CollectionUtils.asMap(key, valueKeyValue);

        data.put(ObjectMetaDataManager.NAME_FIELD, name);
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, account.getId());
        data.put(ObjectMetaDataManager.KIND_FIELD, kind);

        return resourceDao.createAndSchedule(Network.class, data);
    }


    protected Map<String, Network> getNetworksByKind(Account account) {
        Map<String, Network> result = new HashMap<>();

        for (Network network : objectManager.find(Network.class,
                NETWORK.ACCOUNT_ID, account.getId(),
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
