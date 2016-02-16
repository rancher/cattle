package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class ServiceAgentDelegateHandler extends AgentBasedProcessHandler {

    @Inject
    DynamicSchemaDao dynamicSchemaDao;

    @Override
    public String[] getProcessNames() {
        return new String[] {
                ServiceDiscoveryConstants.PROCESS_SERVICE_ACTIVATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_DEACTIVATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_CREATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_REMOVE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_UPDATE,
        };
    }

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        List<Long> agentIds = dynamicSchemaDao.getAgentForService((Service)state.getResource());
        Collections.sort(agentIds);

        return agentIds.size() == 0 ? null : agentIds.get(0);
    }

    @Override
    public List<String> getProcessDataKeys() {
        return Arrays.asList(InstanceConstants.PROCESS_DATA_NO_OP);
    }

}