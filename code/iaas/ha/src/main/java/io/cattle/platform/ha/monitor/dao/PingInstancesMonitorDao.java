package io.cattle.platform.ha.monitor.dao;

import java.util.Map;

public interface PingInstancesMonitorDao {

    Map<String, String> getInstances(long agentId);

    Long getAgentIdForInstanceHostMap(String instanceHostMap);

}
