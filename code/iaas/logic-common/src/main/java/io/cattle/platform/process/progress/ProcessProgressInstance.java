package io.cattle.platform.process.progress;

import io.cattle.platform.engine.process.ProcessState;

public interface ProcessProgressInstance {

    void init(ProcessState state, int... checkpointWeights);

    void checkPoint(String name);

    void progress(Integer progress);

    void messsage(String message);

    String getCurrentCheckpoint();

}