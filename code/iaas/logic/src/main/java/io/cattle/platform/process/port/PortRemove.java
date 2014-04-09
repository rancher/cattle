package io.cattle.platform.process.port;

import javax.inject.Named;

import static io.cattle.platform.core.model.tables.PortTable.*;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

@Named
public class PortRemove extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        return new HandlerResult(PORT.PUBLIC_IP_ADDRESS_ID, null,
                PORT.PRIVATE_IP_ADDRESS_ID, null).withShouldContinue(true);
    }

}
