package io.cattle.platform.object.process;

import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.Map;

public interface ObjectProcessManager {

    String getStandardProcessName(StandardProcess process, Object object);

    String getStandardProcessName(StandardProcess process, String type);

    ProcessInstance createProcessInstance(String processName, Object resource, Map<String, Object> data);

    void scheduleProcessInstance(String processName, Object resource, Map<String, Object> data);

    void scheduleStandardProcess(StandardProcess process, Object resource, Map<String, Object> data);

    ExitReason executeStandardProcess(StandardProcess process, Object resource, Map<String, Object> data);

    ExitReason executeProcess(String processName, Object resource, Map<String, Object> data);

    void scheduleProcessInstanceAsync(String processName, Object resource, Map<String, Object> data);

    void scheduleStandardProcessAsync(StandardProcess process, Object resource, Map<String, Object> data);

    String getProcessName(Object resource, StandardProcess process);

    String getProcessName(Object resource, String processName);

    void scheduleStandardChainedProcessAsync(StandardProcess from, StandardProcess to, Object resource, Map<String, Object> data);

    void scheduleStandardChainedProcess(StandardProcess from, StandardProcess to, Object resource, Map<String, Object> data);

    default void remove(Object resource, Map<String, Object> data) {
        if (!StandardStates.REMOVING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcessAsync(StandardProcess.REMOVE, resource, data);
        }
    }

    default void pause(Object resource, Map<String, Object> data) {
        if (!StandardStates.PAUSING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcessAsync(StandardProcess.PAUSE, resource, data);
        }
    }

    default void create(Object resource, Map<String, Object> data) {
        if (!StandardStates.CREATING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcessAsync(StandardProcess.CREATE, resource, data);
        }
    }

    default void createThenActivate(Object resource, Map<String, Object> data) {
        if (!StandardStates.ACTIVATING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardChainedProcessAsync(StandardProcess.CREATE, StandardProcess.ACTIVATE, resource, data);
        }
    }

    default void deactivateThenRemove(Object resource, Map<String, Object> data) {
        if (!StandardStates.REMOVING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardChainedProcessAsync(StandardProcess.DEACTIVATE, StandardProcess.REMOVE, resource, data);
        }
    }

    default void activate(Object resource, Map<String, Object> data) {
        if (!StandardStates.ACTIVATING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcessAsync(StandardProcess.ACTIVATE, resource, data);
        }
    }

    default void stopThenRemove(Object resource, Map<String, Object> data) {
        if (!StandardStates.REMOVING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardChainedProcessAsync(StandardProcess.STOP, StandardProcess.REMOVE, resource, data);
        }
    }

    default void deactivate(Object resource, Map<String, Object> data) {
        if (!StandardStates.DEACTIVATING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcessAsync(StandardProcess.DEACTIVATE, resource, data);
        }
    }

    default void update(Object resource, Map<String, Object> data) {
        if (!StandardStates.UPDATING.equals(ObjectUtils.getState(resource)) &&
             !StandardStates.UPDATING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcessAsync(StandardProcess.UPDATE, resource, data);
        }
    }

    default void start(Object resource, Map<String, Object> data) {
        if (!StandardStates.STARTING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcessAsync(StandardProcess.START, resource, data);
        }
    }

    default void stop(Object resource, Map<String, Object> data) {
        if (!StandardStates.STOPPING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcessAsync(StandardProcess.STOP, resource, data);
        }
    }

    default void error(Object resource, Map<String, Object> data) {
        if (!StandardStates.ERRORING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcessAsync(StandardProcess.ERROR, resource, data);
        }
    }

    default void executeCreateThenActivate(Object obj, Map<String, Object> data) {
        if (StandardStates.ACTIVE.equals(ObjectUtils.getState(obj))) {
            return;
        }
        try {
            executeStandardProcess(StandardProcess.CREATE, obj, data);
        } catch (ProcessCancelException e) {
           // ignore
        } catch (ProcessNotFoundException e) {
            // ignore
        }
        executeStandardProcess(StandardProcess.ACTIVATE, obj, data);
    }

    default void executeDeactivateThenRemove(Object obj, Map<String, Object> data) {
        if (ObjectUtils.getRemoved(obj) != null) {
            return;
        }
        try {
            executeStandardProcess(StandardProcess.DEACTIVATE, obj, data);
        } catch (ProcessCancelException e) {
           // ignore
        } catch (ProcessNotFoundException e) {
            // ignore
        }
        executeStandardProcess(StandardProcess.REMOVE, obj, data);
    }

    default void executeDeactivateThenScheduleRemove(Object obj, Map<String, Object> data) {
        if (ObjectUtils.getRemoved(obj) != null) {
            return;
        }
        try {
            executeStandardProcess(StandardProcess.DEACTIVATE, obj, data);
        } catch (ProcessCancelException e) {
           // ignore
        } catch (ProcessNotFoundException e) {
            // ignore
        }
        remove(obj, data);
    }

    default void executeDeactivate(Object obj, Map<String, Object> data) {
        executeStandardProcess(StandardProcess.DEACTIVATE, obj, data);
    }

}