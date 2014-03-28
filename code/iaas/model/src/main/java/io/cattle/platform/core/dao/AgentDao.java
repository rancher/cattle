package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Agent;

public interface AgentDao {

    Agent findNonRemovedByUri(String uri);

}
