package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.process.ExitReason;

public interface ProcessInstance {

    Long getId();

    ExitReason execute();

    void schedule();

    boolean isRunningLogic();

    ExitReason getExitReason();

}