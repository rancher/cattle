package io.cattle.platform.inator.factory;

import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.resource.pool.ResourcePoolManager;

import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.concurrent.ScheduledExecutorService;

public class InatorServices {

    public ObjectManager objectManager;
    public ServiceDao serviceDao;
    public GenericResourceDao resourceDao;
    public ObjectProcessManager processManager;
    public ObjectMetaDataManager objectMetadataManager;
    public JsonMapper jsonMapper;
    public NetworkService networkService;
    public IdFormatter idFormatter;
    public LockManager lockManager;
    public AllocationHelper allocationHelper;
    public ResourcePoolManager poolManager;
    public InstanceDao instanceDao;
    public HostDao hostDao;
    public DataDao dataDao;
    public LoopManager loopManager;
    public MetadataManager metadataManager;
    public ScheduledExecutorService scheduledExecutorService;
    public ClusterDao clusterDao;

    public void triggerDeploymentUnitReconcile(Long id) {
        if (id == null) {
            return;
        }
        loopManager.kick(LoopFactory.DU_RECONCILE, ServiceConstants.KIND_DEPLOYMENT_UNIT, id, null);
    }

    public void triggerServiceReconcile(Long id) {
        if (id == null) {
            return;
        }
        loopManager.kick(LoopFactory.RECONCILE, ServiceConstants.KIND_SERVICE, id, null);
    }

}
