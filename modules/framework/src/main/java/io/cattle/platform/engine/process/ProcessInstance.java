package io.cattle.platform.engine.process;

import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.manager.impl.ProcessRecord;

public interface ProcessInstance {

    Long getId();

    String getName();

    String getResourceId();

    Object getResource();

    ExitReason execute();

    ExitReason resume();

    ProcessRecord getProcessRecord();

    ExitReason getExitReason();

    ProcessManager getProcessManager();

}