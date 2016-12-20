package io.cattle.iaas.healthcheck.process;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.*;

import io.cattle.iaas.healthcheck.service.HealthcheckService;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceRemovePostHandler extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    ServiceDao serviceDao;
    @Inject
    HealthcheckService healthcheckService;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        HealthcheckInstance hi = objectManager.findAny(HealthcheckInstance.class, HEALTHCHECK_INSTANCE.INSTANCE_ID,
                instance.getId(),
                HEALTHCHECK_INSTANCE.REMOVED, null);

        if (hi == null) {
            return null;
        }

        List<? extends HealthcheckInstanceHostMap> hostMaps = objectManager.find(HealthcheckInstanceHostMap.class,
                HEALTHCHECK_INSTANCE_HOST_MAP.HEALTHCHECK_INSTANCE_ID,
                hi.getId(),
                HEALTHCHECK_INSTANCE_HOST_MAP.REMOVED, null);


        for (HealthcheckInstanceHostMap hostMap : hostMaps) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, hostMap, null);
        }

        objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, hi, null);
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
