package io.cattle.platform.process.cluster;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.addon.RegistrationToken;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.StackConstants;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessDelayException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.ProxyUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.cattle.platform.core.model.Tables.*;

public class ClusterProcessManager {

    private static final DynamicStringProperty DEFAULT_CLUSTER = ArchaiusUtil.getString("default.cluster");

    private static final String[] STACK_CHECK_FIELDS = new String[] {
            "answers",
            "externalId",
            "labels",
            "templates",
    };

    private static final Class<?>[] REMOVE_TYPES = new Class<?>[]{
            Account.class,
            Service.class,
            Stack.class,
            Agent.class,
            Host.class,
            HostTemplate.class,
            StoragePool.class,
            Volume.class,
            Network.class,
            GenericObject.class,
            Instance.class,
    };

    JsonMapper jsonMapper;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    ClusterDao clusterDao;
    GenericResourceDao resourceDao;
    ResourceMonitor resourceMonitor;

    public ClusterProcessManager(ObjectManager objectManager, ObjectProcessManager processManager, ClusterDao clusterDao, JsonMapper jsonMapper, GenericResourceDao resourceDao, ResourceMonitor resourceMonitor) {
        this.jsonMapper = jsonMapper;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.clusterDao = clusterDao;
        this.resourceDao = resourceDao;
        this.resourceMonitor = resourceMonitor;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        Cluster cluster = (Cluster) state.getResource();
        Account account = clusterDao.getOwnerAcccountForCluster(cluster);
        if (account == null) {
            account = clusterDao.createOwnerAccount(cluster);
        }

        ListenableFuture<?> future = updateStack(cluster, account);

        cluster = clusterDao.assignTokens(cluster);
        return new HandlerResult(
                ClusterConstants.FIELD_REGISTRATION, registrationToken(cluster))
                .withFuture(future);
    }

    public HandlerResult update(ProcessState state, ProcessInstance process) {
        Cluster cluster = (Cluster) state.getResource();
        return new HandlerResult(updateStack(cluster, clusterDao.getOwnerAcccountForCluster(cluster)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ListenableFuture<?> updateStack(Cluster cluster, Account account) {
        List<Map> stacks = DataAccessor.fieldObjectList(cluster, ClusterConstants.FIELD_SYSTEM_STACK, Map.class);
        if (stacks == null || stacks.size() == 0) {
            stacks = defaultStacks();
        }

        List<Stack> toWait = new ArrayList<>();
        List<ListenableFuture<Stack>> futures = new ArrayList<>();
        Map<String, Stack> existingStacks = objectManager.find(Stack.class,
                STACK.ACCOUNT_ID, account.getId(),
                STACK.REMOVED, null).stream()
                .collect(Collectors.toMap(Stack::getName, Function.identity()));

        for (Map stackMap : stacks) {
            Stack stack = ProxyUtils.proxy(stackMap, Stack.class);
            if (StringUtils.isBlank(stack.getName())) {
                continue;
            }

            Stack existing = existingStacks.get(stack.getName());
            if (existing == null) {
                stackMap.put(ObjectMetaDataManager.ACCOUNT_FIELD, account.getId());
                stackMap.put(ObjectMetaDataManager.CLUSTER_FIELD, cluster.getId());
                toWait.add(resourceDao.createAndSchedule(Stack.class, stackMap));
            } else {
                setClusterErrorMessageIfNeeded(cluster, existing);

                Map<String, Object> existingMap = stackToCompareMap(existing);
                boolean changed = false;
                for (String key : STACK_CHECK_FIELDS) {
                    if (!Objects.equals(stackMap.get(key), existingMap.get(key))) {
                        changed = true;
                        break;
                    }
                }

                if (changed) {
                    toWait.add(resourceDao.updateAndSchedule(existing, stackMap));
                }
            }
        }

        for (Stack stack : toWait) {
            ListenableFuture<Stack> stackFuture = resourceMonitor.waitForState(stack,
                    CommonStatesConstants.ACTIVE,
                    CommonStatesConstants.ERROR);

            stackFuture = AsyncUtils.andThen(stackFuture, (futureStack) -> {
                if (CommonStatesConstants.ERROR.equals(futureStack.getState())) {
                    throw new ProcessDelayException(null);
                }
                return futureStack;
            });

            futures.add(stackFuture);
        }

        return futures.size() == 0 ? null : AsyncUtils.afterAll(futures);
    }

    private void setClusterErrorMessageIfNeeded(Cluster cluster, Stack existing) {
        if (!CommonStatesConstants.ERROR.equals(existing.getState())) {
            return;
        }

        String stackTransitioningMessage = DataAccessor.fieldString(existing, ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD);
        if (StringUtils.isBlank(stackTransitioningMessage)) {
            return;
        }

        DataAccessor.setField(cluster, ObjectMetaDataManager.TRANSITIONING_FIELD, ObjectMetaDataManager.TRANSITIONING_ERROR_OVERRIDE);
        DataAccessor.setField(cluster, ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD,
                "Stack error: " + existing.getName() + ": " + stackTransitioningMessage);
    }

    private Map<String, Object> stackToCompareMap(Stack stack) {
        return CollectionUtils.asMap(
                "answers", DataAccessor.fieldMapRO(stack, StackConstants.FIELD_ANSWERS),
                "externalId", stack.getExternalId(),
                "labels", DataAccessor.fieldMapRO(stack, StackConstants.FIELD_LABELS),
                "templates", DataAccessor.fieldMapRO(stack, StackConstants.FIELD_LABELS));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map> defaultStacks() {
        Map<String, Object> cluster;
        try {
            cluster = jsonMapper.readValue(DEFAULT_CLUSTER.get());
        } catch (IOException e) {
            return Collections.emptyList();
        }
        Object stacks = cluster.get(ClusterConstants.FIELD_SYSTEM_STACK);
        return stacks == null ? Collections.emptyList() : jsonMapper.convertCollectionValue(stacks, List.class, Map.class);

    }

    private RegistrationToken registrationToken(Cluster cluster) {
        Credential cred = objectManager.loadResource(Credential.class, DataAccessor.fieldLong(cluster, ClusterConstants.FIELD_REGISTRATION_ID));
        if (cred == null) {
            return null;
        }

        return new RegistrationToken(cred);
    }

    public HandlerResult postRemove(ProcessState state, ProcessInstance process) {
        Cluster cluster = (Cluster)state.getResource();
        removeClusterResources(cluster);
        removeCredentials(cluster);
        return new HandlerResult(ClusterConstants.FIELD_REGISTRATION, (Object[])null);
    }

    protected void removeCredentials(Cluster cluster) {
        Long credId = DataAccessor.fieldLong(cluster, ClusterConstants.FIELD_REGISTRATION_ID);
        Credential cred = objectManager.loadResource(Credential.class, credId);
        if (cred != null && cred.getRemoved() == null) {
            processManager.remove(cred, null);
        }
    }

    protected void removeClusterResources(Cluster cluster) {
        for (Class<?> clz : REMOVE_TYPES) {
            for (Object obj : list(cluster, clz)) {
                if (obj instanceof Instance) {
                    Instance instance = (Instance)obj;
                    deleteAgentAccount(instance.getAgentId());
                    processManager.stopThenRemove(instance, null);
                } else {
                    processManager.deactivateThenRemove(obj, null);
                }
            }
        }
    }

    protected <T> List<T> list(Cluster cluster, Class<T> type) {
        return objectManager.find(type,
                ObjectMetaDataManager.REMOVED_FIELD, null,
                ObjectMetaDataManager.CLUSTER_FIELD, cluster.getId());
    }

    protected void deleteAgentAccount(Long agentId) {
        if (agentId == null) {
            return;
        }

        Agent agent = objectManager.loadResource(Agent.class, agentId);
        Account account  = objectManager.loadResource(Account.class, agent.getAccountId());
        if (account == null) {
            return;
        }

        processManager.executeDeactivateThenScheduleRemove(account, null);
    }

}
