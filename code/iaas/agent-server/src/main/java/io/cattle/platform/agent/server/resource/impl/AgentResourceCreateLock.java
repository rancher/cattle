package io.cattle.platform.agent.server.resource.impl;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class AgentResourceCreateLock extends AbstractLockDefinition {

    public AgentResourceCreateLock(String uuid) {
        super("AGENT.RESOUCE.CREATE." + uuid);
    }

}
