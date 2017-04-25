package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.service.ServiceDataManager;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;
import io.cattle.platform.servicediscovery.deployment.lookups.DeploymentUnitLookup;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitReconcileTrigger extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    DeploymentUnitManager duMgr;
    @Inject
    List<DeploymentUnitLookup> deploymentUnitLookups;
    @Inject
    ServiceDataManager svcDataMgr;
    @Inject
    ServiceDao svcDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_STOP,
                InstanceConstants.PROCESS_REMOVE,
                InstanceConstants.PROCESS_ERROR, HostConstants.PROCESS_REMOVE,
                AgentConstants.PROCESS_RECONNECT, AgentConstants.PROCESS_DECONNECT,
                AgentConstants.PROCESS_FINISH_RECONNECT };
    }

    private static final List<String> CLEANUP_PROCESSES = Arrays.asList(InstanceConstants.PROCESS_ERROR,
            HostConstants.PROCESS_REMOVE,
            AgentConstants.PROCESS_RECONNECT, AgentConstants.PROCESS_DECONNECT);

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        if (process.getName().equalsIgnoreCase(InstanceConstants.PROCESS_ERROR)) {
            Instance instance = (Instance) state.getResource();
            if (instance.getServiceId() == null && instance.getDeploymentUnitId() != null) {
                svcDataMgr.leaveDeploymentUnit(instance);
                return null;
            }
        }

        List<DeploymentUnit> units = new ArrayList<>();
        if (state.getResource() instanceof DeploymentUnit) {
            units.add((DeploymentUnit) state.getResource());
        } else {
            for (DeploymentUnitLookup lookup : deploymentUnitLookups) {
                boolean transitioniongOnly = Arrays.asList(AgentConstants.PROCESS_RECONNECT,
                        AgentConstants.PROCESS_DECONNECT).contains(process.getName());
                Collection<? extends DeploymentUnit> lookupDUs = lookup.getDeploymentUnits(state.getResource(),
                        transitioniongOnly);
                if (lookupDUs != null) {
                    units.addAll(lookupDUs);
                }
            }
        }

        for (DeploymentUnit unit : units) {
            boolean reconcile = true;
            if (duMgr.isGlobal(unit)) {
                if (process.getName().equalsIgnoreCase(HostConstants.PROCESS_REMOVE)) {
                    removeBadUnit(unit);
                    reconcile = false;
                }
            }

            if (CLEANUP_PROCESSES.contains(process.getName())) {
                svcDao.setForCleanup(unit, true);
            }

            if (reconcile) {
                duMgr.scheduleReconcile(unit);
            }
        }
        return null;
    }

    public void removeBadUnit(DeploymentUnit unit) {
        Map<String, Object> data = new HashMap<>();
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_REASON, ServiceConstants.AUDIT_LOG_REMOVE_BAD);
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_LOG_LEVEL, ActivityLog.INFO);
        objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, unit, data);
    }


    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
