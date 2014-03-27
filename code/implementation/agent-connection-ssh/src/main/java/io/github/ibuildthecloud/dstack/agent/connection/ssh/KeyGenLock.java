package io.github.ibuildthecloud.dstack.agent.connection.ssh;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class KeyGenLock extends AbstractLockDefinition {

    public KeyGenLock() {
        super("SSH.KEYGEN");
    }

}
