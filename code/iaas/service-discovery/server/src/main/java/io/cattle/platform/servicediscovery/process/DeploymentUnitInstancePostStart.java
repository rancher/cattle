package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.service.ServiceDataManager;
import io.cattle.platform.util.type.Priority;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitInstancePostStart extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    ServiceDataManager dataMgr;

    @Inject
    InstanceDao instanceDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_START };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        if (!instance.getNativeContainer() && !state.getData().isEmpty() && instance.getFirstRunning() == null) {
            Map<String, Object> originalData = instanceDao.getInstanceSpec(instance);
            if (originalData != null) {
                dataMgr.joinService(instance, originalData);
            }
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
