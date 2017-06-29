package io.cattle.platform.vm.process;

import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.core.constants.ServiceConstants.*;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

public class ServiceVirtualMachinePreCreate implements ProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service)state.getResource();
        if (KIND_VIRTUAL_MACHINE.equals(CollectionUtils.getNestedValue(DataAccessor.fieldMap(service, FIELD_LAUNCH_CONFIG).get("kind")))) {
            return new HandlerResult(ServiceConstants.FIELD_RETAIN_IP, true);
        }
        return null;
    }

}
