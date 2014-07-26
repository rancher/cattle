package io.cattle.platform.process.progress;

import io.cattle.platform.engine.process.ProcessState;

public interface ProcessProgress {

    ProcessProgressInstance get();

    void init(ProcessState state, int... checkpointWeights);

    void checkPoint(String name);

    void progress(Integer progress);

    String getCurrentCheckpoint();

}