package io.cattle.platform.agent.server.ping.dao;

import io.cattle.platform.core.model.Agent;

import java.util.List;

public interface PingDao {

    List<? extends Agent> findAgentsToPing();

}