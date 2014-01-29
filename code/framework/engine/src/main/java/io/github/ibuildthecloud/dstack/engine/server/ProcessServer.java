package io.github.ibuildthecloud.dstack.engine.server;

public interface ProcessServer {

    void runOutstandingJobs();

    void runRemainingTasks(long processId);

}
