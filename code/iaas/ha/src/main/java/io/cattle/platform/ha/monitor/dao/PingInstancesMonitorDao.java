package io.cattle.platform.ha.monitor.dao;

import java.util.Set;

public interface PingInstancesMonitorDao {

    Set<String> getHosts(long agentId);

    Long getAgentIdForInstanceHostMap(String instanceHostMap);

}
