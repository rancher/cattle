package io.github.ibuildthecloud.dstack.lock.definition;

public interface MultiLockDefinition extends LockDefinition {

    LockDefinition[] getLockDefinitions();

}
