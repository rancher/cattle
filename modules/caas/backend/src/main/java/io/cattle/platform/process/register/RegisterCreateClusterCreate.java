package io.cattle.platform.process.register;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.async.utils.AsyncUtils;
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
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.account.AccountProcessManager;
import io.cattle.platform.process.lock.AccountClusterCreateLock;

import java.util.Map;

import static io.cattle.platform.core.model.Tables.*;

public class RegisterCreateClusterCreate implements ProcessHandler {

    LockManager lockManager;
    ObjectManager objectManager;
    ClusterDao clusterDao;
    ResourceMonitor resourceMonitor;
    EventService eventService;

    public RegisterCreateClusterCreate(LockManager lockManager, ObjectManager objectManager, ClusterDao clusterDao, ResourceMonitor resourceMonitor, EventService eventService) {
        this.lockManager = lockManager;
        this.objectManager = objectManager;
        this.clusterDao = clusterDao;
        this.resourceMonitor = resourceMonitor;
        this.eventService = eventService;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        GenericObject resource = (GenericObject) state.getResource();
        if (!GenericObjectConstants.KIND_REGISTER.equals(resource.getKind())) {
            return null;
        }

        String key = resource.getKey();
        if (key == null) {
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


        Account finalAccount = account;
        ListenableFuture<?> future = AsyncUtils.andThen(resourceMonitor.waitForState(cluster, CommonStatesConstants.ACTIVE), (ignore) -> {
            AccountProcessManager.disconnectClients(eventService, finalAccount);
            return ignore;
        });

        return new HandlerResult(GENERIC_OBJECT.CLUSTER_ID, cluster.getId()).withFuture(future);
    }

}
