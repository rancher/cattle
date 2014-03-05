package io.github.ibuildthecloud.dstack.object.process;

import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;

import java.util.Map;

public interface ObjectProcessManager {
    String getStandardProcessName(StandardProcess process, String type);

    ProcessInstance createProcessInstance(String processName, Object resource, Map<String, Object> data);

    void scheduleProcessInstance(String processName, Object resource, Map<String, Object> data);

    void scheduleStandardProcess(StandardProcess process, Object resource, Map<String, Object> data);

    ExitReason executeStandardProcess(StandardProcess process, Object resource, Map<String, Object> data);
}
