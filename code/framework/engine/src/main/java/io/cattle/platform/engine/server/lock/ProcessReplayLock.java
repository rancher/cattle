package io.cattle.platform.engine.server.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ProcessReplayLock extends AbstractLockDefinition {

    public ProcessReplayLock() {
        super("PROCESS.REPLAY");
    }

}
