package io.cattle.platform.agent.server.resource.impl;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class AgentResourceCreateLock extends AbstractBlockingLockDefintion {

    public AgentResourceCreateLock(Agent agent) {
        super("AGENT.RESOUCE.CREATE." + agent.getId());
    }

}
