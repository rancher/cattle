package io.cattle.platform.object.process;

import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.Predicate;
import io.cattle.platform.engine.process.ProcessInstance;

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
}
