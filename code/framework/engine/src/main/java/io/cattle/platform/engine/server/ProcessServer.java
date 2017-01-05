package io.cattle.platform.engine.server;

import io.cattle.platform.engine.process.ProcessInstance;

public interface ProcessServer {

    void runOutstandingJobs();

    Long getNextTask(ProcessInstance instance);

}
