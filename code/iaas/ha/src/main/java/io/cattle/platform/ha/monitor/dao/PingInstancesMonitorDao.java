package io.cattle.platform.ha.monitor.dao;

import io.cattle.platform.ha.monitor.model.KnownInstance;

import java.util.Map;

public interface PingInstancesMonitorDao {

    Map<String, KnownInstance> getInstances(long agentId);

    Long getAgentIdForInstanceHostMap(String instanceHostMap);

}
