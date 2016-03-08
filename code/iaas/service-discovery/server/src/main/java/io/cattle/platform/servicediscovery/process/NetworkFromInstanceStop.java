package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

public class NetworkFromInstanceStop extends AbstractObjectProcessHandler {

    @Inject
    ServiceDiscoveryService sdService;
    @Inject
    JsonMapper jsonMapper;

    @Inject
    NetworkDao ntwkDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_RESTART };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        if (instance.getDeploymentUnitUuid() == null) {
            return null;
        }
        
        List<Instance> dependants = objectManager.find(Instance.class, INSTANCE.REMOVED, null,
                INSTANCE.DEPLOYMENT_UNIT_UUID, instance.getDeploymentUnitUuid(), INSTANCE.NETWORK_CONTAINER_ID,
                instance.getId());
        if (dependants.isEmpty()) {
            return null;
        }

        List<String> invalidStates = Arrays.asList(CommonStatesConstants.REMOVING, InstanceConstants.STATE_ERROR,
                InstanceConstants.STATE_ERRORING, InstanceConstants.STATE_STOPPING, InstanceConstants.STATE_STOPPED);
        for (Instance dependant : dependants) {
            if (!invalidStates.contains(dependant.getState())) {
                objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, dependant, null);

            }
        }

        return null;
    }
}