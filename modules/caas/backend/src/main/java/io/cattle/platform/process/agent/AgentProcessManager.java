package io.cattle.platform.process.agent;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;

public class AgentProcessManager {

    private static final DynamicIntProperty PING_RETRY = ArchaiusUtil.getInt("agent.activate.ping.retries");
    private static final DynamicLongProperty PING_TIMEOUT = ArchaiusUtil.getLong("agent.activate.ping.timeout");
    private static final DynamicLongProperty PING_DISCONNECT_TIMEOUT = ArchaiusUtil.getLong("agent.disconnect.after.seconds");
    private static final DynamicBooleanProperty CREATE_ACCOUNT = ArchaiusUtil.getBoolean("process.agent.create.create.account");

    private static final Logger log = LoggerFactory.getLogger(AgentProcessManager.class);

    AccountDao accountDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    AgentLocator agentLocator;
    EventService eventService;
    SchemaFactory schemaFactory;

    public AgentProcessManager(AccountDao accountDao, ObjectManager objectManager, ObjectProcessManager processManager,
            AgentLocator agentLocator, EventService eventService, SchemaFactory schemaFactory) {
        this.accountDao = accountDao;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.agentLocator = agentLocator;
        this.eventService = eventService;
        this.schemaFactory = schemaFactory;
    }

    public HandlerResult activate(ProcessState state, ProcessInstance process) {
        /* This will save the time */
        checkDisconnect(state);

        Agent agent = (Agent) state.getResource();
        Instance instance = objectManager.findAny(Instance.class, INSTANCE.AGENT_ID, agent.getId());

        /* Don't ping non-system container agent instances */
        if (instance != null) {
            return null;
        }

        for (String prefix : AgentConstants.AGENT_IGNORE_PREFIXES) {
            if (agent.getUri() == null || agent.getUri().startsWith(prefix)) {
                return new HandlerResult();
            }
        }

        boolean waitFor = DataAccessor.fromDataFieldOf(agent)
                .withScope(AgentProcessManager.class)
                .withKey("waitForPing")
                .withDefault(process.getName().equals(AgentConstants.PROCESS_RECONNECT))
                        .as(Boolean.class);

        RemoteAgent remoteAgent = agentLocator.lookupAgent(agent);
        final ListenableFuture<? extends Event> future = remoteAgent.call(AgentUtils.newPing(agent)
                .withOption(Ping.STATS, true)
                .withOption(Ping.RESOURCES, true), new EventCallOptions(PING_RETRY.get(), PING_TIMEOUT.get())
                .withRetryCallback((event) -> {
                    Agent newAgent = objectManager.reload(agent);
                    if (AgentConstants.STATE_DISCONNECTING.equals(newAgent.getState()) ||
                            CommonStatesConstants.DEACTIVATING.equals(newAgent.getState())) {
                        throw new TimeoutException();
                    }
                    return event;
                }));
        future.addListener(new NoExceptionRunnable() {
            @Override
            protected void doRun() {
                try {
                    Event resp = future.get();
                    EventVO<?> respCopy = new EventVO<>(resp);
                    respCopy.setName("ping.reply");
                    eventService.publish(respCopy);
                } catch (Exception e) {
                }
            }
        }, MoreExecutors.directExecutor());


        if (waitFor) {
            try {
                AsyncUtils.get(future);
            } catch (TimeoutException e) {
                HandlerResult result = checkDisconnect(state);
                if (result == null) {
                    throw e;
                } else {
                    return result;
                }
            }
        }
        HandlerResult result = new HandlerResult();
        if (process.getName().equalsIgnoreCase(AgentConstants.PROCESS_RECONNECT)) {
            result.setChainProcessName(AgentConstants.PROCESS_FINISH_RECONNECT);
        }
        return result;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent) state.getResource();
        Long accountId = agent.getAccountId();

        if (accountId != null || !CREATE_ACCOUNT.get()) {
            return new HandlerResult(AGENT.ACCOUNT_ID, accountId);
        }

        DataAccessor.fromMap(state.getData()).withScope(AccountConstants.class).withKey(AccountConstants.OPTION_CREATE_APIKEY).set(true);

        DataAccessor.fromMap(state.getData()).withScope(AccountConstants.class).withKey(AccountConstants.OPTION_CREATE_APIKEY_KIND).set(
                CredentialConstants.KIND_AGENT_API_KEY);

        List<? extends String> roles =
                DataAccessor.fromDataFieldOf(agent).withKey(AgentConstants.DATA_REQUESTED_ROLES).withDefault(Collections.EMPTY_LIST)
                        .asList(String.class);
        sortRoles(roles);

        String primaryRole = getPrimaryRole(roles); // note null is an acceptable value
        Account account = createPrimaryAccount(agent, primaryRole);
        processManager.executeCreateThenActivate(account, state.getData());

        Map<String, Object> data = new HashMap<>();
        data.put(AgentConstants.FIELD_ACCOUNT_ID, account.getId());

        List<? extends String> secondaryRoles = getSecondaryRoles(roles);
        List<Long> authedRoleAccounts = createSecondaryAccounts(agent, secondaryRoles, state);
        if (!authedRoleAccounts.isEmpty()) {
            data.put(AgentConstants.FIELD_AUTHORIZED_ROLE_ACCOUNTS, authedRoleAccounts);
        }

        return new HandlerResult(data);
    }

    public HandlerResult remove(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent)state.getResource();

        for (String type : AgentUtils.AGENT_RESOURCES.get()) {
            Class<?> clz = schemaFactory.getSchemaClass(type);
            if (clz == null) {
                log.error("Failed to find class for [{}]", type);
                continue;
            }


            for (Object obj : objectManager.children(agent, clz)) {
                if (obj instanceof StoragePool) {
                    StoragePool sp = (StoragePool)obj;
                    if (StoragePoolConstants.TYPE.equals(sp.getKind())) {
                        // Don't automatically delete shared storage pools
                        continue;
                    }
                }
                processManager.executeDeactivateThenScheduleRemove(obj, null);
            }
        }

        if (agent.getAccountId() != null) {
            processManager.executeDeactivateThenScheduleRemove(objectManager.loadResource(Account.class, agent.getAccountId()), null);
        }

        List<Long> authedRoleAccountIds = DataAccessor.fieldLongList(agent, AgentConstants.FIELD_AUTHORIZED_ROLE_ACCOUNTS);
        for (Long accountId : authedRoleAccountIds) {
            processManager.executeDeactivateThenScheduleRemove(objectManager.loadResource(Account.class, accountId), null);
        }

        return null;
    }


    protected HandlerResult checkDisconnect(ProcessState state) {
        DataAccessor acc = DataAccessor.fromMap(state.getData()).withScope(AgentProcessManager.class).withKey("start");
        Long startTime = acc.as(Long.class);
        if (startTime == null) {
            startTime = System.currentTimeMillis();
            acc.set(startTime);
        }

        if (PING_DISCONNECT_TIMEOUT.get() * 1000L < (System.currentTimeMillis() - startTime)) {
            return new HandlerResult().withChainProcessName(AgentConstants.PROCESS_DECONNECT);
        }

        return null;
    }

    protected Account createPrimaryAccount(Agent agent, String role) {
        if (agent.getAccountId() != null) {
            return objectManager.loadResource(Account.class, agent.getAccountId());
        }

        String uuid = getAccountUuid(agent);
        Account account = accountDao.findByUuid(uuid);

        if (account == null) {
            Object data = createAccountDataWithActAsValue(role);
            account = objectManager.create(Account.class,
                    ACCOUNT.UUID, uuid,
                    ACCOUNT.DATA, data,
                    ACCOUNT.KIND, "agent");
        }

        return account;
    }

    protected List<Long> createSecondaryAccounts(Agent agent, List<? extends String> roles, ProcessState state) {
        if (roles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> authedRoleAccountIds = DataAccessor.fieldLongList(agent, AgentConstants.FIELD_AUTHORIZED_ROLE_ACCOUNTS);
        if (authedRoleAccountIds != null && !authedRoleAccountIds.isEmpty()) {
            return authedRoleAccountIds;
        }

        authedRoleAccountIds = new ArrayList<>();
        for (String role : roles) {
            String uuid = getAccountUuid(agent, role);
            Account account = accountDao.findByUuid(uuid);

            Map<String, Object> data = createAccountDataWithActAsValue(role);
            data.put(AccountConstants.DATA_AGENT_OWNER_ID, agent.getId());

            if (account == null) {
                account = objectManager.create(Account.class,
                        ACCOUNT.UUID, uuid,
                        ACCOUNT.DATA, data,
                        ACCOUNT.KIND, "agent");
            }
            processManager.executeCreateThenActivate(account, state.getData());
            authedRoleAccountIds.add(account.getId());
        }

        return authedRoleAccountIds;
    }

    Map<String, Object> createAccountDataWithActAsValue(String r) {
        if ("environment".equals(r)) {
            return CollectionUtils.asMap(AccountConstants.DATA_ACT_AS_RESOURCE_ACCOUNT, true);
        } else if ("environmentAdmin".equals(r)) {
            return CollectionUtils.asMap(AccountConstants.DATA_ACT_AS_RESOURCE_ADMIN_ACCOUNT, true);
        }
        return new HashMap<>();
    }

    private void sortRoles(List<? extends String> roles) {
        Collections.sort(roles, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                if (lhs.equals(rhs)) {
                    return 0;
                }

                if ("agent".equals(lhs)) {
                    return -1;
                } else if ("agent".equals(rhs)) {
                    return 1;
                }

                if ("environmentAdmin".equals(lhs)) {
                    return -1;
                } else if ("environmentAdmin".equals(rhs)) {
                    return 1;
                }

                return 0;
            }
        });
    }

    private String getPrimaryRole(List<? extends String> roles) {
        if (roles.isEmpty()) {
            return null;
        }
        return roles.get(0);
    }

    private List<? extends String> getSecondaryRoles(List<? extends String> roles) {
        if (roles.isEmpty()) {
            return roles;
        } else if (roles.size() == 1){
            return new ArrayList<>();
        }
        return roles.subList(1, roles.size());
    }

    protected String getAccountUuid(Agent agent, String role) {
        return "agentAccount" + agent.getId() + "-" + role;
    }

    protected String getAccountUuid(Agent agent) {
        return "agentAccount" + agent.getId();
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

}
