package io.github.ibuildthecloud.dstack.engine.process.impl;

import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class ProcessLock extends AbstractLockDefinition {

    public ProcessLock(ProcessInstance process) {
        super(process.getId() == null ? null : "PROCESS.INSTANCE." + process.getId());
    }

}
