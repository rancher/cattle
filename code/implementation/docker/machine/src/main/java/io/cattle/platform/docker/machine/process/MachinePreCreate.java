package io.cattle.platform.docker.machine.process;

import static io.cattle.platform.core.model.tables.PhysicalHostTable.*;
import static io.cattle.platform.docker.machine.constants.MachineConstants.*;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
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
        PhysicalHost physHost = (PhysicalHost)state.getResource();

        if ( !StringUtils.equalsIgnoreCase(physHost.getKind(), MACHINE_KIND) ) {
            return null;
        }

        Map<Object, Object> newFields = new HashMap<Object, Object>();

        if ( StringUtils.isEmpty(physHost.getExternalId()) ) {
            String externalId = UUID.randomUUID().toString();
            newFields.put(PHYSICAL_HOST.EXTERNAL_ID, externalId);
        }

        Map<String, Object> fields = DataUtils.getFields(physHost);
        for ( Map.Entry<String, Object> field : fields.entrySet() ) {
            if ( StringUtils.endsWithIgnoreCase(field.getKey(), CONFIG_FIELD_SUFFIX) && field.getValue() != null ) {
                String driver = StringUtils.removeEndIgnoreCase(field.getKey(), CONFIG_FIELD_SUFFIX);
                newFields.put(DRIVER_FIELD, driver);
                break;
            }
        }

        if ( !newFields.isEmpty() ) {
            return new HandlerResult(newFields);
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
