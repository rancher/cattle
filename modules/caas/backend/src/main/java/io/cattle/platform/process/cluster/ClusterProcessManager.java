package io.cattle.platform.process.cluster;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.addon.K8sClientConfig;
import io.cattle.platform.core.addon.RegistrationToken;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ProjectConstants;
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
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lifecycle.util.LifecycleException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.util.ProxyUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.cattle.platform.core.model.Tables.*;

public class ClusterProcessManager {

    private static final DynamicStringProperty DEFAULT_CLUSTER = ArchaiusUtil.getString("default.cluster.template");
    private static final DynamicStringProperty NETES_ADDRESS = ArchaiusUtil.getString("netes.address");

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
    IdFormatter idFormatter;

    public ClusterProcessManager(ObjectManager objectManager, ObjectProcessManager processManager, ClusterDao clusterDao, JsonMapper jsonMapper, GenericResourceDao resourceDao, ResourceMonitor resourceMonitor, IdFormatter idFormatter) {
        this.jsonMapper = jsonMapper;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.clusterDao = clusterDao;
        this.resourceDao = resourceDao;
        this.resourceMonitor = resourceMonitor;
        this.idFormatter = idFormatter;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        Cluster cluster = (Cluster) state.getResource();
        Account account = clusterDao.getOwnerAcccountForCluster(cluster.getId());
        if (account == null) {
            account = clusterDao.createOwnerAccount(cluster);
        }

        Account defaultProject = clusterDao.getDefaultProject(cluster);
        if (defaultProject == null) {
            clusterDao.createDefaultProject(cluster);
        }

        cluster = clusterDao.assignTokens(cluster);
        return new HandlerResult(ClusterConstants.FIELD_REGISTRATION, registrationToken(cluster));
    }

    public HandlerResult activate(ProcessState state, ProcessInstance process) {
        Cluster cluster = (Cluster) state.getResource();

        setClusterMode(cluster);
        ListenableFuture<?> future = createEnvironments(cluster);

        return new HandlerResult(future);
    }

    public HandlerResult deployStack(ProcessState state, ProcessInstance process) {
        Cluster cluster = (Cluster) state.getResource();
        Account account = clusterDao.getOwnerAcccountForCluster(cluster.getId());

        try {
            updateStack(cluster, account);
            return null;
        } catch (LifecycleException e) {
            return new HandlerResult().withChainProcessName(ClusterConstants.PROCESS_ERROR);
        }
    }

    private ListenableFuture<?> createEnvironments(Cluster cluster) {
        List<ListenableFuture<Account>> futures = new ArrayList<>();
        for (Map<String, Object> stack : getStacks(cluster)) {
            String project = Objects.toString(stack.get(ProjectConstants.TYPE), null);
            if (StringUtils.isBlank(project)) {
                continue;
            }

            Account account = clusterDao.createOrGetProjectByName(cluster, project, null);
            futures.add(resourceMonitor.waitForState(account, CommonStatesConstants.ACTIVE, CommonStatesConstants.INACTIVE,
                    CommonStatesConstants.REMOVED));
        }

        return AsyncUtils.afterAll(futures);

    }

    private void setClusterMode(Cluster cluster) {
        String orchestration = DataAccessor.fieldString(cluster, ClusterConstants.FIELD_ORCHESTRATION);
        if (StringUtils.isNotBlank(orchestration)) {
            return;
        }

        for (Map<String, Object> stackMap : getStacks(cluster)) {
            Stack stack = ProxyUtils.proxy(stackMap, Stack.class);
            if ("kubernetes-support".equals(stack.getName())) {
                String clusterId = idFormatter.formatId(ClusterConstants.TYPE, cluster.getId()).toString();
                String address = String.format(NETES_ADDRESS.get(), clusterId);
                objectManager.setFields(cluster,
                        ClusterConstants.FIELD_K8S_CLIENT_CONFIG, new K8sClientConfig(address),
                        ClusterConstants.FIELD_ORCHESTRATION, ClusterConstants.ORCH_KUBERNETES,
                        CLUSTER.EMBEDDED, true);
                break;
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map<String, Object>> getStacks(Cluster cluster) {
        List<Map> stacks = DataAccessor.fieldObjectList(cluster, ClusterConstants.FIELD_SYSTEM_STACK, Map.class);
        if (stacks == null || stacks.size() == 0) {
            stacks = defaultStacks();
        }

        return (List<Map<String, Object>>)(List)stacks;
    }

    private void updateStack(Cluster cluster, Account account) throws LifecycleException {
        for (Map<String, Object> stackMap : getStacks(cluster)) {
            Account projectAccount = account;
            String project = Objects.toString(stackMap.get(ProjectConstants.TYPE), null);
            if (StringUtils.isNotBlank(project)) {
                projectAccount = clusterDao.createOrGetProjectByName(cluster, project, null);
            }

            Stack stack = ProxyUtils.proxy(stackMap, Stack.class);
            if (StringUtils.isBlank(stack.getName())) {
                continue;
            }

            Stack existingStack = objectManager.findAny(Stack.class,
                    STACK.ACCOUNT_ID, projectAccount.getId(),
                    STACK.NAME, stack.getName(),
                    STACK.REMOVED, null);

            if (existingStack == null) {
                stackMap.put(ObjectMetaDataManager.ACCOUNT_FIELD, projectAccount.getId());
                stackMap.put(ObjectMetaDataManager.CLUSTER_FIELD, cluster.getId());
                resourceDao.createAndSchedule(Stack.class, stackMap);
            }
        }
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
