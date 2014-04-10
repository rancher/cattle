package io.cattle.platform.engine.process;

import io.cattle.platform.engine.manager.ProcessManager;

public interface ProcessInstance {

    Long getId();

    String getName();

    String getResourceId();

    ExitReason execute();

    boolean isRunningLogic();

    ExitReason getExitReason();

    ProcessManager getProcessManager();

}