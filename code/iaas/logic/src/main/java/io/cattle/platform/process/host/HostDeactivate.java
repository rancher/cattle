package io.cattle.platform.process.host;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.lock.AgentCleanupLock;
import io.cattle.platform.process.util.ProcessHelpers;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostDeactivate extends AbstractDefaultProcessHandler {

    LockManager lockManager;

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        if (host.getAgentId() == null) {
            return null;
        }

        lockManager.lock(new AgentCleanupLock(host.getAgentId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Agent agent = objectManager.loadResource(Agent.class, host.getAgentId());
                if (agent == null) {
                    return;
                }

                List<Host> children = ProcessHelpers.getNonRemovedChildren(objectManager, agent, Host.class);
                if (children.size() > 1) {
                    return;
                }

                deactivate(agent, state.getData());
            }
        });

        return null;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

}
