package io.cattle.platform.agent.connection.ssh;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class KeyGenLock extends AbstractLockDefinition {

    public KeyGenLock() {
        super("SSH.KEYGEN");
    }

}
