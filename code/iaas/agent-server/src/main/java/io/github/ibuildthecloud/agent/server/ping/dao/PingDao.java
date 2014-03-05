package io.github.ibuildthecloud.agent.server.ping.dao;

import java.util.List;

import io.github.ibuildthecloud.dstack.core.model.Agent;

public interface PingDao {

    List<? extends Agent> findAgentsToPing();


}
