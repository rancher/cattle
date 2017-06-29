package io.cattle.platform.agent.server.ping.dao;

import java.util.List;

public interface PingDao {

    List<Long> findAgentsToPing();

}