package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lifecycle.InstanceLifecycleManager;
import io.cattle.platform.lifecycle.util.LifecycleException;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.process.containerevent.ContainerEventCreate;
import io.cattle.platform.util.exception.ExecutionException;

import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstancePreStart extends AbstractDefaultProcessHandler {

    @Inject
    InstanceLifecycleManager instanceLifecycle;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance) state.getResource();

        try {
            instanceLifecycle.preStart(instance);
        } catch (LifecycleException e) {
            handleStartError(objectProcessManager, state, instance, new ExecutionException(e, instance));
        }

        return null;
    }

    public static HandlerResult handleStartError(ObjectProcessManager objectProcessManager, ProcessState state, Instance instance, ExecutionException e) {
        if ((InstanceCreate.isCreateStart(state) || instance.getFirstRunning() == null) && !ContainerEventCreate.isNativeDockerStart(state)) {
            HashMap<String, Object> data = new HashMap<>();
            data.put(InstanceConstants.PROCESS_DATA_ERROR, true);
            objectProcessManager.scheduleProcessInstance(InstanceConstants.PROCESS_STOP, instance,
                    ProcessUtils.chainInData(data, InstanceConstants.PROCESS_STOP,
                            InstanceConstants.PROCESS_ERROR));
        } else {
            objectProcessManager.stop(instance, null);
        }

        e.setResources(state.getResource());
        throw e;
    }

}
