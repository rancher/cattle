package io.cattle.platform.object.process;

import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.Predicate;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.Map;

public interface ObjectProcessManager {

    String getStandardProcessName(StandardProcess process, Object object);

    String getStandardProcessName(StandardProcess process, String type);

    ProcessInstance createProcessInstance(String processName, Object resource, Map<String, Object> data);

    void scheduleProcessInstance(String processName, Object resource, Map<String, Object> data);

    void scheduleProcessInstance(String processName, Object resource, Map<String, Object> data, Predicate predicate);

    void scheduleStandardProcess(StandardProcess process, Object resource, Map<String, Object> data);

    void scheduleStandardProcess(StandardProcess process, Object resource, Map<String, Object> data, Predicate predicate);

    ExitReason executeStandardProcess(StandardProcess process, Object resource, Map<String, Object> data);

    ExitReason executeProcess(String processName, Object resource, Map<String, Object> data);

    void scheduleProcessInstanceAsync(String processName, Object resource, Map<String, Object> data);

    void scheduleStandardProcessAsync(StandardProcess process, Object resource, Map<String, Object> data);

    String getProcessName(Object resource, StandardProcess process);

    void scheduleStandardChainedProcessAsync(StandardProcess from, StandardProcess to, Object resource, Map<String, Object> data);

    void scheduleStandardChainedProcess(StandardProcess from, StandardProcess to, Object resource, Map<String, Object> data);

    default void remove(Object resource, Map<String, Object> data) {
        if (!StandardStates.REMOVING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcess(StandardProcess.REMOVE, resource, data);
        }
    }

    default void create(Object resource, Map<String, Object> data) {
        if (!StandardStates.CREATING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcess(StandardProcess.CREATE, resource, data);
        }
    }

    default void createThenActivate(Object resource, Map<String, Object> data) {
        if (!StandardStates.ACTIVATING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardChainedProcess(StandardProcess.CREATE, StandardProcess.ACTIVATE, resource, data);
        }
    }

    default void activate(Object resource, Map<String, Object> data) {
        if (!StandardStates.ACTIVATING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcess(StandardProcess.ACTIVATE, resource, data);
        }
    }

    default void deactivate(Object resource, Map<String, Object> data) {
        if (!StandardStates.DEACTIVATING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcess(StandardProcess.DEACTIVATE, resource, data);
        }
    }

    default void purge(Object resource, Map<String, Object> data) {
        if (!StandardStates.PURGING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcess(StandardProcess.PURGE, resource, data);
        }
    }

    default void update(Object resource, Map<String, Object> data) {
        if (!StandardStates.UPDATING_ACTIVE.equals(ObjectUtils.getState(resource)) &&
             !StandardStates.UPDATING_INACTIVE.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcess(StandardProcess.UPDATE, resource, data);
        }
    }

    default void start(Object resource, Map<String, Object> data) {
        if (!StandardStates.STARTING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcess(StandardProcess.START, resource, data);
        }
    }

    default void stop(Object resource, Map<String, Object> data) {
        if (!StandardStates.STOPPING.equals(ObjectUtils.getState(resource))) {
            scheduleStandardProcess(StandardProcess.STOP, resource, data);
        }
    }

}