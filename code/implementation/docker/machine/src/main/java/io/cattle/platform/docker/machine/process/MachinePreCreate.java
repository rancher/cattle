package io.cattle.platform.docker.machine.process;

import static io.cattle.platform.core.constants.MachineConstants.*;
import static io.cattle.platform.core.model.tables.PhysicalHostTable.*;

import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.process.host.HostCreateToProvision;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public class MachinePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "physicalhost.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        PhysicalHost physHost = (PhysicalHost) state.getResource();

        if (!StringUtils.equalsIgnoreCase(physHost.getKind(), KIND_MACHINE)) {
            return null;
        }

        Map<Object, Object> newFields = new HashMap<Object, Object>();

        if (StringUtils.isEmpty(physHost.getExternalId())) {
            String externalId = UUID.randomUUID().toString();
            newFields.put(PHYSICAL_HOST.EXTERNAL_ID, externalId);
        } else {
            newFields.put(PHYSICAL_HOST.EXTERNAL_ID, physHost.getExternalId());
        }

        String driver = HostCreateToProvision.getDriver(physHost);
        if (driver != null) {
            newFields.put(FIELD_DRIVER, driver);
        }

        return new HandlerResult(newFields);
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
