package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.manager.ProcessManager;

public interface ProcessInstance {

    Long getId();

    String getName();

    ExitReason execute();

    void schedule();

    boolean isRunningLogic();

    ExitReason getExitReason();

    ProcessManager getProcessManager();

}