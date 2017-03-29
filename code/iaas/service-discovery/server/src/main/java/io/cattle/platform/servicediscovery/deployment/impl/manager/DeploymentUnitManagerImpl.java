package io.cattle.platform.servicediscovery.deployment.impl.manager;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.docker.transform.DockerTransformer;
import io.cattle.platform.engine.idempotent.IdempotentRetryException;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.service.ServiceDataManager;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;
import io.cattle.platform.servicediscovery.deployment.impl.lock.DeploymentUnitLock;
import io.cattle.platform.servicediscovery.deployment.impl.unit.AbstractDeploymentUnit;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.exception.DeploymentUnitReconcileException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import DeploymentUnitFactory.DeploymentUnitFactory;

public class DeploymentUnitManagerImpl implements DeploymentUnitManager {
    private static final String DU_RECONCILE = "deployment-unit-update";

    @Inject
    LockManager lockManager;
    @Inject
    ObjectProcessManager objectProcessMgr;
    @Inject
    ServiceDiscoveryService sdSvc;
    @Inject
    ObjectManager objectMgr;
    @Inject
    ResourceMonitor resourceMntr;
    @Inject
    ServiceExposeMapDao expMapDao;
    @Inject
    ServiceDao svcDao;
    @Inject
    IdFormatter idFrmt;
    @Inject
    ActivityService actvtyService;
    @Inject
    ConfigItemStatusManager itemManager;
    @Inject
    ActivityService activitySvc;
    @Inject
    JsonMapper jMapper;
    @Inject
    VolumeDao volDao;
    @Inject
    DockerTransformer dockerTransformer;
    @Inject
    ServiceDataManager dataMgr;
    @Inject
    GenericResourceDao rDao;

    public final class DeploymentUnitManagerContext {
        final public ObjectManager objectManager = objectMgr;
        final public ResourceMonitor resourceMonitor = resourceMntr;
        final public ObjectProcessManager objectProcessManager = objectProcessMgr;
        final public ServiceDiscoveryService sdService = sdSvc;
        final public ServiceExposeMapDao exposeMapDao = expMapDao;
        final public ServiceDao serviceDao = svcDao;
        final public ActivityService activityService = actvtyService;
        final public IdFormatter idFormatter = idFrmt;
        final public JsonMapper jsonMapper = jMapper;
        final public VolumeDao volumeDao = volDao;
        final public DockerTransformer transformer = dockerTransformer;
        final public ServiceDataManager svcDataMgr = dataMgr;
        final public GenericResourceDao resourceDao = rDao;
    }

    @Override
    public void deactivate(DeploymentUnit unit) {
        getDeploymentUnit(unit).stop();
    }

    @Override
    public void remove(DeploymentUnit unit, String reason, String level) {
        getDeploymentUnit(unit).remove(reason, level);
        // cleanup instances set for upgrade
        cleanupUpgradedInstances(unit);
    }

    protected void reconcile(final DeploymentUnit unit) {
        if (!isActiveUnit(unit)) {
            return;
        }
        lockManager.lock(new DeploymentUnitLock(unit), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                io.cattle.platform.servicediscovery.deployment.DeploymentUnit impl = getDeploymentUnit(unit);
                boolean log = impl instanceof AbstractDeploymentUnit;
                if (log) {
                    activitySvc.info(impl.getStatus());
                }
                if (!impl.needToReconcile()) {
                    if (log) {
                        activitySvc.info("Deployment unit already reconciled");
                    }
                    return;
                }
                activateDeploymentUnit(unit);
                sdSvc.incrementExecutionCount(unit);
                impl.deploy();
                // reload in case something has changed
                impl = getDeploymentUnit(unit);
                if (impl.needToReconcile()) {
                    throw new DeploymentUnitReconcileException("Need to restart deployment unit reconcile");
                }
            }
        });
    }

    private boolean isActiveUnit(DeploymentUnit unit) {
        List<String> activeStates = Arrays.asList(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE,
                CommonStatesConstants.UPDATING_ACTIVE);
        return activeStates.contains(unit.getState());
    }

    @Override
    public void scheduleReconcile(DeploymentUnit unit) {
        ConfigUpdateRequest request = ConfigUpdateRequest.forResource(DeploymentUnit.class, unit.getId());
        request.addItem(DU_RECONCILE);
        request.withDeferredTrigger(true);
        itemManager.updateConfig(request);
    }

    @Override
    public void deploymentUnitUpdate(ConfigUpdate update) {
        final Client client = new Client(DeploymentUnit.class, new Long(update.getResourceId()));
        itemManager.runUpdateForEvent(DU_RECONCILE, update, client, new Runnable() {
            @Override
            public void run() {
                final DeploymentUnit unit = objectMgr.loadResource(DeploymentUnit.class, client.getResourceId());
                if (!isActiveUnit(unit)) {
                    return;
                }
                if (unit.getServiceId() != null) {
                    final Service service = objectMgr.loadResource(Service.class, unit.getServiceId());
                    if (ServiceUtil.isServiceValidForReconcile(service)) {
                        activitySvc.run(service, unit, "deploymentunit.trigger", "Re-evaluating deployment unit state",
                                new Runnable() {
                            @Override
                            public void run() {
                                reconcile(unit);
                            }
                        });
                    }
                } else {
                    reconcile(unit);
                }
            }
        });
    }

    private void activateDeploymentUnit(final DeploymentUnit unit) {
        try {
            if (unit.getState().equalsIgnoreCase(CommonStatesConstants.ACTIVE)) {
                objectProcessMgr.scheduleStandardProcess(StandardProcess.UPDATE, unit, null);
            }
        } catch (IdempotentRetryException ex) {
        }
    }

    @Override
    public void activate(DeploymentUnit unit) {
        reconcile(unit);
    }

    @Override
    public boolean isUnhealthy(DeploymentUnit unit) {
        return getDeploymentUnit(unit).isUnhealthy();
    }

    @Override
    public boolean isInit(DeploymentUnit unit) {
        return getDeploymentUnit(unit).isHealthCheckInitializing();
    }

    io.cattle.platform.servicediscovery.deployment.DeploymentUnit getDeploymentUnit(DeploymentUnit unit) {
        return DeploymentUnitFactory.fetchDeploymentUnit(unit, new DeploymentUnitManagerContext());
    }

    public void cleanupUpgradedInstances(DeploymentUnit unit) {
        List<? extends Instance> instances = expMapDao.getDeploymentUnitInstancesSetForUpgrade(unit);
        for (Instance instance : instances) {
            try {
                objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_REMOVE,
                        instance, null);
            } catch (ProcessCancelException ex) {
                // in case instance was manually restarted
                objectProcessMgr.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_REMOVE));
            }
        }
    }
}
