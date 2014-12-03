package io.cattle.platform.process.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class AgentCleanupLock extends AbstractBlockingLockDefintion {

    public AgentCleanupLock(Long agentId) {
        super(agentId == null ? null : "AGENT.CLEANUP." + agentId);
    }

}
