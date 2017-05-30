package io.cattle.platform.object.process;

import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstance;
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

    default void stopAndRemove(Object resource, Map<String, Object> data) {
        if (!StandardStates.REMOVING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardChainedProcessAsync(StandardProcess.STOP, StandardProcess.REMOVE, resource, data);
        }
    }

    default void deactivateAndRemove(Object resource, Map<String, Object> data) {
        scheduleStandardChainedProcessAsync(StandardProcess.DEACTIVATE, StandardProcess.REMOVE, resource, data);
    }

    default void deactivate(Object resource, Map<String, Object> data) {
        if (!StandardStates.DEACTIVATING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcessAsync(StandardProcess.DEACTIVATE, resource, data);
        }
    }

    default void update(Object resource, Map<String, Object> data) {
        if (!StandardStates.UPDATING_ACTIVE.equals(ObjectUtils.getState(resource)) &&
             !StandardStates.UPDATING_INACTIVE.equals(ObjectUtils.getState(resource))) {
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

}