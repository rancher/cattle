package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessListener;

public interface ProcessLogicRegistry {

    ProcessListener getProcessListener(String name);

    ProcessHandler getProcessHandler(String name);
}
