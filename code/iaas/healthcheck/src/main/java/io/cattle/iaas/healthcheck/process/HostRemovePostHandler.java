package io.cattle.iaas.healthcheck.process;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostRemovePostHandler extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Inject
    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { HostConstants.PROCESS_REMOVE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Host host = (Host) state.getResource();
        List<? extends HealthcheckInstanceHostMap> healthHostMaps = mapDao.findNonRemoved(
                HealthcheckInstanceHostMap.class,
                Host.class, host.getId());
        for (HealthcheckInstanceHostMap healthHostMap : healthHostMaps) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, healthHostMap, null);
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
