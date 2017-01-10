package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.service.ServiceDataManager;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitManager;
import io.cattle.platform.servicediscovery.deployment.lookups.DeploymentUnitLookup;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
                InstanceConstants.PROCESS_REMOVE, ServiceConstants.PROCESS_DU_UPDATE_UNHEALTHY,
                InstanceConstants.PROCESS_ERROR, HostConstants.PROCESS_REMOVE,
                AgentConstants.PROCESS_RECONNECT, AgentConstants.PROCESS_DECONNECT };
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
            if (CLEANUP_PROCESSES.contains(process.getName())) {
                objectManager.setFields(unit, ServiceConstants.FIELD_DEPLOYMENT_UNIT_CLEANUP, true);
            }
            duMgr.scheduleReconcile(unit);
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
