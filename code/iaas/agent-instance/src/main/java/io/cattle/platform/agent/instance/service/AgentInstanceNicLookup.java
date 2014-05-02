package io.cattle.platform.agent.instance.service;

import io.cattle.platform.core.model.Nic;

public interface AgentInstanceNicLookup {

    Nic getNic(Object obj);

}
