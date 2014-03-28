package io.cattle.platform.lock.definition;

public interface BlockingLockDefinition extends LockDefinition {

    long getWait();

}
