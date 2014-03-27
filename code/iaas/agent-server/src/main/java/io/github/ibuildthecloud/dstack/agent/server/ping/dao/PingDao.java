package io.github.ibuildthecloud.dstack.agent.server.ping.dao;

import io.github.ibuildthecloud.dstack.core.model.Agent;

import java.util.List;

public interface PingDao {

    List<? extends Agent> findAgentsToPing();


}
