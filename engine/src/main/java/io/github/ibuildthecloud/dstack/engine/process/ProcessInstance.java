package io.github.ibuildthecloud.dstack.engine.process;

import io.github.ibuildthecloud.dstack.engine.process.ExitReason;

public interface ProcessInstance {

    Long getId();

    ExitReason execute();

    boolean isRunningLogic();

    ExitReason getExitReason();

}