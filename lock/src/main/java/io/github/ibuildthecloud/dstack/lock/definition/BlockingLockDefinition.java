package io.github.ibuildthecloud.dstack.lock.definition;

public interface BlockingLockDefinition extends LockDefinition {

    long getWait();

}
