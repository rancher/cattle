package io.cattle.platform.agent.server.ping.dao;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.StoragePool;

import java.util.List;
import java.util.Map;

public interface PingDao {

    List<? extends Agent> findAgentsToPing();

    Map<String,Host> getHosts(long agentId);

    Map<String,StoragePool> getStoragePools(long agentId);

    Map<String,PhysicalHost> getPhysicalHosts(long agentId);

}