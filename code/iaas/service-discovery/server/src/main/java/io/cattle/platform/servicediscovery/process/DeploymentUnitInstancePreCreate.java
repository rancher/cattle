package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DeploymentUnitInstancePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Inject
    InstanceDao instanceDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_CREATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        if (!instance.getNativeContainer() && !state.getData().isEmpty()) {
            instanceDao.createRevision(instance, state.getData());
        }
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}

