package io.cattle.platform.process.cluster;

import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Cluster;
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
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.List;

public class ClusterProcessManager {

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

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    ClusterDao clusterDao;

    public ClusterProcessManager(ObjectManager objectManager, ObjectProcessManager processManager, ClusterDao clusterDao) {
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.clusterDao = clusterDao;
    }

    public HandlerResult preCreate(ProcessState state, ProcessInstance process) {
        Cluster cluster = (Cluster)state.getResource();
        Account account = clusterDao.getOwnerAcccountForCluster(cluster);
        if (account == null) {
            clusterDao.createOwnerAccount(cluster);
        }

        return null;
    }

    public HandlerResult postRemove(ProcessState state, ProcessInstance process) {
        Cluster cluster = (Cluster)state.getResource();
        removeClusterResources(cluster);
        return null;
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
