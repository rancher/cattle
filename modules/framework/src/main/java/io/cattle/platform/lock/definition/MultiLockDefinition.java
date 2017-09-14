package io.cattle.platform.lock.definition;

public interface MultiLockDefinition extends LockDefinition {

    LockDefinition[] getLockDefinitions();

}
