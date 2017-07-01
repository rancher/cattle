package io.cattle.platform.process.instance;

import io.cattle.platform.containersync.ContainerSync;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lifecycle.InstanceLifecycleManager;
import io.cattle.platform.lifecycle.util.LifecycleException;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.exception.ExecutionException;

import java.util.HashMap;

public class InstanceProcessManager {

    private static final String START = "start";

    InstanceLifecycleManager instanceLifecycle;
    ObjectProcessManager processManager;

    public InstanceProcessManager(InstanceLifecycleManager instanceLifecycle, ObjectProcessManager processManager) {
        super();
        this.instanceLifecycle = instanceLifecycle;
        this.processManager = processManager;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        setCreateStart(state);

        Instance instance = (Instance) state.getResource();
        try {
            instanceLifecycle.create(instance);
        } catch (LifecycleException e) {
            processManager.remove(instance, null);
            throw new ExecutionException(e, instance);
        }

        HandlerResult result = new HandlerResult();
        if (shouldStart(state, instance)) {
            result.setChainProcessName(InstanceConstants.PROCESS_START);
        }

        return result;
    }

    public HandlerResult postStart(ProcessState state, ProcessInstance process) {
        instanceLifecycle.postStart((Instance) state.getResource());
        return null;
    }

    public HandlerResult postStop(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();

        String chainProcess = null;
        boolean start = DataAccessor.fromMap(state.getData())
                .withScope(InstanceProcessManager.class)
                .withKey(START)
                .withDefault(false)
                .as(Boolean.class);

        if (start) {
            chainProcess = InstanceConstants.PROCESS_START;
        } else if (Boolean.TRUE.equals(state.getData().get(InstanceConstants.REMOVE_OPTION)) ||
                InstanceConstants.ON_STOP_REMOVE.equals(instance.getInstanceTriggeredStop())) {
            chainProcess = processManager.getStandardProcessName(StandardProcess.REMOVE, instance);
        }

        instanceLifecycle.postStop(instance, chainProcess == null);
        return new HandlerResult().withChainProcessName(chainProcess);
    }

    public HandlerResult preRemove(ProcessState state, ProcessInstance process) {
        instanceLifecycle.preRemove((Instance) state.getResource());
        return null;
    }

    public HandlerResult preStart(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();

        try {
            instanceLifecycle.preStart(instance);
        } catch (LifecycleException e) {
            handleStartError(processManager, state, instance, new ExecutionException(e, instance));
        }

        return null;
    }

    public HandlerResult restart(ProcessState state, ProcessInstance process) {
        DataAccessor.fromMap(state.getData())
            .withScope(InstanceProcessManager.class)
            .withKey(START)
            .set(true);
        return new HandlerResult().withChainProcessName(InstanceConstants.PROCESS_STOP);
    }


    public static HandlerResult handleStartError(ObjectProcessManager objectProcessManager, ProcessState state, Instance instance, ExecutionException e) {
        if ((isCreateStart(state) || instance.getFirstRunning() == null) && !ContainerSync.isNativeDockerStart(state)) {
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



    private boolean shouldStart(ProcessState state, Instance instance) {
        Boolean shouldStart = DataAccessor
                .fromDataFieldOf(state)
                .withKey(InstanceConstants.FIELD_START_ON_CREATE)
                .as(Boolean.class);
        if (shouldStart != null) {
            return shouldStart;
        }
        return DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_START_ON_CREATE)
                .withDefault(true)
                .as(Boolean.class);
    }

    private static boolean isCreateStart(ProcessState state) {
        Boolean startOnCreate = DataAccessor
                .fromMap(state.getData())
                .withScope(InstanceProcessManager.class)
                .withKey(InstanceConstants.FIELD_START_ON_CREATE)
                .as(Boolean.class);

        return startOnCreate == null ? false : startOnCreate;
    }

    private static void setCreateStart(ProcessState state) {
        DataAccessor
            .fromMap(state.getData())
            .withScope(InstanceProcessManager.class)
            .withKey(InstanceConstants.FIELD_START_ON_CREATE)
            .set(true);
    }


}
