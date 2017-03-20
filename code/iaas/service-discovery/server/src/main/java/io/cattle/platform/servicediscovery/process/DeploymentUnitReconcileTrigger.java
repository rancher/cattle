package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
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
                Collection<? extends DeploymentUnit> lookupDUs = lookup.getDeploymentUnits(state.getResource());
                if (lookupDUs != null) {
                    units.addAll(lookupDUs);
                }
            }
        }

        for (DeploymentUnit unit : units) {
            boolean reconcile = true;
            if (isGlobalUnit(unit)) {
                if (process.getName().equalsIgnoreCase(HostConstants.PROCESS_REMOVE)) {
                    removeBadUnit(unit);
                    reconcile = false;
                } else {
                    // do not reconcile global unit on reconnecting host
                    // as it is not going to be rescheduled to the new one
                    reconcile = isActiveHost(unit);
                }
            } else if (CLEANUP_PROCESSES.contains(process.getName())) {
                objectManager.setFields(unit, ServiceConstants.FIELD_DEPLOYMENT_UNIT_CLEANUP, true);
            }

            if (reconcile) {
                duMgr.scheduleReconcile(unit);
            }
        }
        return null;
    }

    boolean isActiveHost(DeploymentUnit unit) {
        Host host = objectManager.loadResource(Host.class,
                Long.valueOf(DataAccessor.fieldMap(unit, InstanceConstants.FIELD_LABELS)
                        .get(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID).toString()));
        if (host == null) {
            return false;
        }
        Agent agent = objectManager.loadResource(Agent.class, host.getAgentId());
        List<String> badAgentStates = Arrays.asList(AgentConstants.STATE_RECONNECTING,
                AgentConstants.STATE_DISCONNECTED, AgentConstants.STATE_DISCONNECTING);
        if (badAgentStates.contains(agent.getState())) {
            return false;
        }
        return true;
    }

    public void removeBadUnit(DeploymentUnit unit) {
        Map<String, Object> data = new HashMap<>();
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_REASON, ServiceConstants.AUDIT_LOG_REMOVE_BAD);
        data.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_REMOVE_LOG_LEVEL, ActivityLog.INFO);
        objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, unit, data);
    }

    protected boolean isGlobalUnit(DeploymentUnit unit) {
        Map<String, Object> unitLabels = DataAccessor.fieldMap(unit, InstanceConstants.FIELD_LABELS);
        return unitLabels.containsKey(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
