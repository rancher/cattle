package io.cattle.platform.ha.monitor;

import io.cattle.platform.eventing.annotation.AnnotatedEventListener;
import io.cattle.platform.eventing.annotation.EventHandler;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.framework.event.Ping;

public interface PingInstancesMonitor extends AnnotatedEventListener {

    @EventHandler
    void pingReply(Ping ping);

    @EventHandler
    void computeInstanceActivateReply(Event event);

}
