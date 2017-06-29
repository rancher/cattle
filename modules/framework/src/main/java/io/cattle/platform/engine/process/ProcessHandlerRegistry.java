package io.cattle.platform.engine.process;

import io.cattle.platform.engine.handler.ProcessHandler;

import java.util.List;

public interface ProcessHandlerRegistry {

    List<ProcessHandler> getHandlers(String processName);

}
