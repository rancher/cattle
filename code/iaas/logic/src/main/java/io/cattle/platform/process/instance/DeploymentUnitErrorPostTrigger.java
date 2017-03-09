package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitErrorPostTrigger extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    ObjectManager objMgr;
    @Inject
    ServiceDao svcDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_ERROR, HostConstants.PROCESS_REMOVE,
                AgentConstants.PROCESS_RECONNECT, AgentConstants.PROCESS_DECONNECT };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        List<DeploymentUnit> units = new ArrayList<>();
        List<String> skipStates = Arrays.asList(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING,
                CommonStatesConstants.REMOVING);
        if (state.getResource() instanceof Instance) {
            Instance instance = (Instance) state.getResource();
            DeploymentUnit unit = objectManager.findAny(DeploymentUnit.class, DEPLOYMENT_UNIT.UUID,
                    instance.getDeploymentUnitUuid(), DEPLOYMENT_UNIT.REMOVED, null);
            if (unit == null) {
                return null;
            }
            units.add(unit);
        } else if (state.getResource() instanceof Agent) {
            Agent agent = (Agent) state.getResource();
            Host host = objMgr.findAny(Host.class, HOST.AGENT_ID, agent.getId(), HOST.REMOVED, null);
            if (host == null) {
                return null;
            }
            units.addAll(svcDao.getUnitsOnHost(host, true));
        } else if (state.getResource() instanceof Host) {
            Host host = (Host) state.getResource();
            units.addAll(svcDao.getUnitsOnHost(host, false));
        }
        
        for (DeploymentUnit unit : units) {
            if (skipStates.contains(unit.getState())) {
                continue;
            }
            // only put service units to error
            if (unit.getServiceId() != null) {
                objectManager.setFields(unit, ServiceConstants.FIELD_DEPLOYMENT_UNIT_CLEANUP, true);
                objectProcessManager.scheduleProcessInstanceAsync(ServiceConstants.PROCESS_DU_ERROR, unit, null);
            }
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
