package io.cattle.platform.process.register;

import io.cattle.platform.core.addon.K8sClientConfig;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.GenericObjectConstants;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.lock.AccountClusterCreateLock;

import java.util.Map;

import static io.cattle.platform.core.model.Tables.*;

public class RegisterCreateClusterCreate implements ProcessHandler {

    LockManager lockManager;
    ObjectManager objectManager;
    ClusterDao clusterDao;
    ResourceMonitor resourceMonitor;

    public RegisterCreateClusterCreate(LockManager lockManager, ObjectManager objectManager, ClusterDao clusterDao, ResourceMonitor resourceMonitor) {
        this.lockManager = lockManager;
        this.objectManager = objectManager;
        this.clusterDao = clusterDao;
        this.resourceMonitor = resourceMonitor;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        GenericObject resource = (GenericObject) state.getResource();
        if (!GenericObjectConstants.KIND_REGISTER.equals(resource.getKind())) {
            return null;
        }

        String key = resource.getKey();
        if (key == null || resource.getClusterId() == null) {
            return null;
        }

        if (resource.getClusterId() == null) {
            Account account = objectManager.loadResource(Account.class, resource.getAccountId());
            return lockManager.lock(new AccountClusterCreateLock(account), () -> createCluster(account, resource));
        }

        return null;
    }

    private HandlerResult createCluster(Account account, GenericObject resource) {
        account = objectManager.reload(account);
        if (account.getClusterId() != null) {
            return new HandlerResult(GENERIC_OBJECT.CLUSTER_ID, resource.getClusterId());
        }

        K8sClientConfig clientConfig = DataAccessor.field(resource, GenericObjectConstants.FIELD_K8S_CLIENT_CONFIG, K8sClientConfig.class);
        Map<String, Object> obj = DataAccessor.fieldMapRO(resource, GenericObjectConstants.FIELD_K8S_CLIENT_CONFIG);
        Cluster cluster = clusterDao.createClusterForAccount(account, obj.size() == 0 ? null : clientConfig);


        return new HandlerResult(
                GENERIC_OBJECT.CLUSTER_ID, cluster.getId())
                .withFuture(resourceMonitor.waitForState(cluster, CommonStatesConstants.ACTIVE));
    }

}
