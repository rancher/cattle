package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lifecycle.InstanceLifecycleManager;
import io.cattle.platform.lifecycle.util.LifecycleException;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.util.exception.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceCreate extends AbstractDefaultProcessHandler {

    @Inject
    InstanceLifecycleManager instanceLifecycleManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        setCreateStart(state);

        Instance instance = (Instance) state.getResource();
        try {
            instanceLifecycleManager.create(instance);
        } catch (LifecycleException e) {
            objectProcessManager.remove(instance, null);
            throw new ExecutionException(e, instance);
        }

        HandlerResult result = new HandlerResult();
        if (shouldStart(state, instance)) {
            result.setChainProcessName(InstanceConstants.PROCESS_START);
        }

        return result;
    }

    private boolean shouldStart(ProcessState state, Instance instance) {
        Boolean shouldStart = DataAccessor
                .fromDataFieldOf(state)
                .withKey(InstanceConstants.FIELD_START_ON_CREATE).as(Boolean.class);
        if (shouldStart != null) {
            return shouldStart;
        }
        return DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_START_ON_CREATE)
                .withDefault(true)
                .as(Boolean.class);
    }

    public static boolean isCreateStart(ProcessState state) {
        Boolean startOnCreate = DataAccessor.fromMap(state.getData()).withScope(InstanceCreate.class).withKey(InstanceConstants.FIELD_START_ON_CREATE).as(
                Boolean.class);

        return startOnCreate == null ? false : startOnCreate;
    }

    private void setCreateStart(ProcessState state) {
        DataAccessor.fromMap(state.getData()).withScope(InstanceCreate.class).withKey(InstanceConstants.FIELD_START_ON_CREATE).set(true);
    }

}
