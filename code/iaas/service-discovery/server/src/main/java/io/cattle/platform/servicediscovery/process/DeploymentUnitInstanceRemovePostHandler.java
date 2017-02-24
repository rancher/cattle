package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitInstanceRemovePostHandler extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    ServiceDao svcDao;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    LockManager lockMgr;
    @Inject
    ServiceDiscoveryService sdSvc;
    @Inject
    InstanceDao instanceDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        cleanupHealthcheckMaps(instance);
        sdSvc.removeFromLoadBalancerServices(null, instance);
        leaveDeploymentUnit(instance);
        instanceDao.cleanupInstanceRevisions(instance);
        return null;
    }

    public void leaveDeploymentUnit(Instance instance) {
        if (instance.getDeploymentUnitId() == null) {
            return;
        }
        if (svcDao.isServiceManagedInstance(instance)) {
            return;
        }

        List<? extends Instance> instances = objectManager.find(Instance.class, INSTANCE.DEPLOYMENT_UNIT_ID,
                instance.getDeploymentUnitId(), INSTANCE.REMOVED, null);
        for (Instance i : instances) {
            // do not remove if there are instances assignied to unit
            if (!i.getId().equals(instance.getId()) && !i.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                return;
            }
        }

        DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, instance.getDeploymentUnitId());
        if (unit == null || unit.getRemoved() != null
                || unit.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_REASON, ServiceConstants.AUDIT_LOG_REMOVE_EXTRA);
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_LOG_LEVEL, ActivityLog.INFO);
        try {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, unit, data);
        } catch (ProcessCancelException e) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.DEACTIVATE,
                    unit, ProcessUtils.chainInData(data,
                            ServiceConstants.PROCESS_DU_DEACTIVATE, ServiceConstants.PROCESS_DU_REMOVE));
        }
    }

    private void cleanupHealthcheckMaps(Instance instance) {
        HealthcheckInstance hi = objectManager.findAny(HealthcheckInstance.class, HEALTHCHECK_INSTANCE.INSTANCE_ID,
                instance.getId(),
                HEALTHCHECK_INSTANCE.REMOVED, null);

        if (hi == null) {
            return;
        }

        List<? extends HealthcheckInstanceHostMap> hostMaps = objectManager.find(HealthcheckInstanceHostMap.class,
                HEALTHCHECK_INSTANCE_HOST_MAP.HEALTHCHECK_INSTANCE_ID,
                hi.getId(),
                HEALTHCHECK_INSTANCE_HOST_MAP.REMOVED, null);

        for (HealthcheckInstanceHostMap hostMap : hostMaps) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, hostMap, null);
        }

        objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, hi, null);
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
