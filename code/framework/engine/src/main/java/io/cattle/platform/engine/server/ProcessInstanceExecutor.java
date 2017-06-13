package io.cattle.platform.engine.server;

import io.cattle.platform.engine.process.ProcessInstance;

public interface ProcessInstanceExecutor {

    ProcessInstance execute(long processId);

    ProcessInstance resume(ProcessInstance instance);

}
