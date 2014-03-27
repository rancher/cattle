package io.github.ibuildthecloud.dstack.agent.server.resource.impl;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class AgentResourceCreateLock extends AbstractLockDefinition {

    public AgentResourceCreateLock(String uuid) {
        super("AGENT.RESOUCE.CREATE." + uuid);
    }

}
