package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.List;

public class HealthcheckConfigUpdate extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] {
                "nic.activate",
                "nic.deactivate"
        };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic) state.getResource();
        List<? extends HealthcheckInstanceHostMap> hostMaps = objectManager.find(
                HealthcheckInstanceHostMap.class, HEALTHCHECK_INSTANCE_HOST_MAP.INSTANCE_ID, nic.getInstanceId(),
                HEALTHCHECK_INSTANCE_HOST_MAP.REMOVED, null, HEALTHCHECK_INSTANCE_HOST_MAP.STATE, CommonStatesConstants.ACTIVE);
        for (HealthcheckInstanceHostMap hostMap : hostMaps) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.UPDATE, hostMap, null);
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
