package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;

public class ServiceCancelupgrade extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        return new HandlerResult(ServiceDiscoveryConstants.FIELD_UPGRADE, new Object[]{null});
    }

}
