package io.cattle.platform.agent.instance.factory.lock;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class AgentInstanceAgentCreateLock extends AbstractBlockingLockDefintion {

    public AgentInstanceAgentCreateLock(String uri) {
        super("AGENT.INSTANCE.AGENT.CREATE." + uri.hashCode());
    }

}
