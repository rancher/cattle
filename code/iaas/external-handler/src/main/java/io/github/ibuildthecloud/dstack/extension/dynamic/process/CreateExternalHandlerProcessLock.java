package io.github.ibuildthecloud.dstack.extension.dynamic.process;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class CreateExternalHandlerProcessLock extends AbstractLockDefinition {

    public CreateExternalHandlerProcessLock(String lockId) {
        super("CREATE.EXTERNAL.HANDLER.PROCESS.LOCK." + lockId);
    }

}
