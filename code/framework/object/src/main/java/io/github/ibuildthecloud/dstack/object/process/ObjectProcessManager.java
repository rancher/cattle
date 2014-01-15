package io.github.ibuildthecloud.dstack.object.process;

import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;

import java.util.Map;

public interface ObjectProcessManager {
    ProcessInstance createProcessInstance(LaunchConfiguration config);

    ProcessInstance createProcessInstance(String processName, Object resource, Map<String, Object> data);

    LaunchConfiguration createLaunchConfiguration(String processName, Object resource, Map<String, Object> data);

    void scheduleStandardProcess(StandardProcess process, Object resource, Map<String, Object> data);

    ExitReason executeStandardProcess(StandardProcess process, Object resource, Map<String, Object> data);
}
