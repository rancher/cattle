package io.github.ibuildthecloud.dstack.engine.server;

public interface ProcessServer {

    Long getId();

    void runOutstandingJobs();

}
