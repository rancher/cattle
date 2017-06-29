package io.cattle.platform.engine.task;

import io.cattle.platform.engine.server.ProcessServer;
import io.cattle.platform.task.Task;

public class ProcessReplayTask implements Task {

    ProcessServer processServer;

    public ProcessReplayTask(ProcessServer processServer) {
        this.processServer = processServer;
    }

    @Override
    public void run() {
        processServer.runOutstandingJobs();
    }

    @Override
    public String getName() {
        return "process.replay";
    }

}
