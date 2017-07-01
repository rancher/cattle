package io.cattle.platform.containersync;

import io.cattle.platform.framework.event.Ping;

public interface PingInstancesMonitor {

    void processPingReply(Ping ping);

}
