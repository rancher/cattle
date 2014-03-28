package io.cattle.platform.engine.server;

public interface ProcessServer {

    void runOutstandingJobs();

    void runRemainingTasks(long processId);

}
