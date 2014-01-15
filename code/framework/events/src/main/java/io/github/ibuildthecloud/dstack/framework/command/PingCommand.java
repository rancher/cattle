package io.github.ibuildthecloud.dstack.core.command;

import io.github.ibuildthecloud.dstack.core.event.CoreEvents;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;

public class PingCommand extends EventVO {

    public PingCommand() {
        setName(CoreEvents.PING);
    }

}
