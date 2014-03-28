package io.cattle.platform.engine.process.impl;

import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ProcessLock extends AbstractLockDefinition {

    public ProcessLock(ProcessInstance process) {
        super(process.getId() == null ? null : "PROCESS.INSTANCE." + process.getId());
    }

}
