package io.cattle.platform.engine.server;

public interface ProcessInstanceDispatcher {

    void execute(Long id, boolean priority);

}
