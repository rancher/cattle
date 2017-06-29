package io.cattle.platform.engine.model;

import io.cattle.platform.engine.process.ProcessInstance;

public interface Trigger {

    void trigger(ProcessInstance process);

}
