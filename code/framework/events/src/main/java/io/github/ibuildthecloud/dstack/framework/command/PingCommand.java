package io.github.ibuildthecloud.dstack.framework.command;

import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.framework.event.FrameworkEvents;

public class PingCommand extends EventVO {

    public PingCommand() {
        setName(FrameworkEvents.PING);
    }

}
