package io.cattle.platform.vm.process;

import static io.cattle.platform.core.constants.InstanceConstants.*;
import static io.cattle.platform.core.constants.ServiceConstants.*;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;

public class ServiceVirtualMachinePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "service.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service)state.getResource();
        if (KIND_VIRTUAL_MACHINE.equals(CollectionUtils.getNestedValue(DataAccessor.fieldMap(service, FIELD_LAUNCH_CONFIG).get("kind")))) {
            return new HandlerResult(ServiceConstants.FIELD_SERVICE_RETAIN_IP, true);
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }
}
