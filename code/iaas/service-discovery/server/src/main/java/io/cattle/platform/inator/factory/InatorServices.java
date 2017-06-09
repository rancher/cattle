package io.cattle.platform.inator.factory;

import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.loop.LoopFactoryImpl;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InatorServices {

    @Inject
    public ObjectManager objectManager;
    @Inject
    public ServiceDao serviceDao;
    @Inject
    public GenericResourceDao resourceDao;
    @Inject
    public ObjectProcessManager processManager;
    @Inject
    public ObjectMetaDataManager metadataManager;
    @Inject
    public JsonMapper jsonMapper;
    @Inject
    public ServiceDiscoveryService sdService;
    @Inject
    public NetworkService networkService;
    @Inject
    public IdFormatter idFormatter;
    @Inject
    public LockManager lockManager;
    @Inject
    public ConfigItemStatusManager itemManager;
    @Inject
    public AllocationHelper allocationHelper;
    @Inject
    public ResourcePoolManager poolManager;
    @Inject
    public RevisionManager revisionManager;
    @Inject
    public InstanceDao instanceDao;
    @Inject
    public HostDao hostDao;
    @Inject
    public DataDao dataDao;
    @Inject
    public LoopManager loopManager;

    public void triggerDeploymentUnitReconcile(Long id) {
        if (id == null) {
            return;
        }
        loopManager.kick(LoopFactoryImpl.DU_RECONCILE, ServiceConstants.KIND_DEPLOYMENT_UNIT, id, null);
    }

    public void triggerServiceReconcile(Long id) {
        if (id == null) {
            return;
        }
        loopManager.kick(LoopFactoryImpl.RECONCILE, ServiceConstants.KIND_SERVICE, id, null);
    }

}
