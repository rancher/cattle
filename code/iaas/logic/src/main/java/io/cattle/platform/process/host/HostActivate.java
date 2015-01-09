package io.cattle.platform.process.host;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class HostActivate extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        if (host.getAgentId() == null) {
            return null;
        }

        Agent agent = objectManager.loadResource(Agent.class, host.getAgentId());
        if (agent == null) {
            return null;
        }

        activate(agent, state.getData());

        return null;
    }

}
