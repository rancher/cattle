package io.github.ibuildthecloud.dstack.core.dao;

import io.github.ibuildthecloud.dstack.core.model.Agent;

public interface AgentDao {

    Agent findNonRemovedByUri(String uri);

}
