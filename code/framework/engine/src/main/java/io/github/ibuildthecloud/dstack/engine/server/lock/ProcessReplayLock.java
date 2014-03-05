package io.github.ibuildthecloud.dstack.engine.server.lock;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class ProcessReplayLock extends AbstractLockDefinition {

    public ProcessReplayLock() {
        super("PROCESS.REPLAY");
    }

}
