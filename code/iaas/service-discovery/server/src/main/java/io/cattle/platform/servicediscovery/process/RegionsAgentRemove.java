package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.RegionService;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class RegionsAgentRemove extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    RegionService regionService;
    @Inject
    JsonMapper jsonMapper;

    @Inject

    @Override
    public String[] getProcessNames() {
        return new String[] { "agent.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Agent agent = (Agent) state.getResource();
        if (agent.getExternalId() != null) {
            return null;
        }
        regionService.deactivateAndRemoveExtenralAgent(agent);

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}

