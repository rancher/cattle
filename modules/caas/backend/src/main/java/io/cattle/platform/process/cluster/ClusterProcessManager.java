package io.cattle.platform.process.cluster;

import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.ObjectProcessManager;

public class ClusterProcessManager {

    ObjectProcessManager processManager;
    ClusterDao clusterDao;

    public ClusterProcessManager(ObjectProcessManager processManager, ClusterDao clusterDao) {
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
        Account account = clusterDao.getOwnerAcccountForCluster(cluster);
        processManager.remove(account, null);
        return null;
    }

}
