package io.github.ibuildthecloud.dstack.engine.server;


public interface ProcessInstanceDispatcher {

    void execute(ProcessServer server, Long id);

}
