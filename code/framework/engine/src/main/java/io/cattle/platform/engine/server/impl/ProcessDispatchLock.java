package io.cattle.platform.engine.server.impl;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ProcessDispatchLock extends AbstractLockDefinition {

    public ProcessDispatchLock(Long id) {
        super("PROCESS.DISPATCH." + id);
    }

}
