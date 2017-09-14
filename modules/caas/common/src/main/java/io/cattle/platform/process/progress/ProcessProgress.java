package io.cattle.platform.process.progress;

import io.cattle.platform.engine.process.ProcessState;

public interface ProcessProgress {

    ProcessProgressInstance get();

    void init(ProcessState state);

    void checkPoint(String name);

}