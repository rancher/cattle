package io.cattle.platform.engine.process;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.impl.ProcessRecord;

public interface ProcessInstance {

    Long getId();

    String getName();

    String getResourceId();

    Object getResource();

    ExitReason execute();

    ProcessRecord getProcessRecord();

    boolean isRunningLogic();

    ExitReason getExitReason();

    ProcessManager getProcessManager();

}