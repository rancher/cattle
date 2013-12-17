package io.github.ibuildthecloud.dstack.command;

import io.github.ibuildthecloud.dstack.core.events.CoreEvents;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;

public class PingCommand extends EventVO {

    public PingCommand() {
        setName(CoreEvents.PING);
    }

}
