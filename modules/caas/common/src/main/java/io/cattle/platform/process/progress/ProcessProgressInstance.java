package io.cattle.platform.process.progress;

import io.cattle.platform.engine.process.ProcessState;

public interface ProcessProgressInstance {

    void init(ProcessState state);

    void checkPoint(String name);

    void messsage(String message);

}