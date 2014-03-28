package io.cattle.platform.extension.dynamic.process;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class CreateExternalHandlerProcessLock extends AbstractLockDefinition {

    public CreateExternalHandlerProcessLock(String lockId) {
        super("CREATE.EXTERNAL.HANDLER.PROCESS.LOCK." + lockId);
    }

}
