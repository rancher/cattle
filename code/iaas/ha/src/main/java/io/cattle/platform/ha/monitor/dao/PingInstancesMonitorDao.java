package io.cattle.platform.ha.monitor.dao;

import java.util.Map;
import java.util.Set;

public interface PingInstancesMonitorDao {

    Map<String, String> getInstances(long agentId);

    Long getAgentIdForInstanceHostMap(String instanceHostMap);

}
