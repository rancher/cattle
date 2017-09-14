package io.cattle.platform.containersync;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.framework.event.Ping;

public interface PingInstancesMonitor {

    void processPingReply(Agent agent, Ping ping);

}
