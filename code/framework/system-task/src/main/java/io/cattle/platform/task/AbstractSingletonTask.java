package io.cattle.platform.task;

import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.lock.definition.LockDefinition;

public abstract class AbstractSingletonTask implements LockDefinition, Task {

    LockDelegator delegator;

    public AbstractSingletonTask(LockDelegator delegator) {
        this.delegator = delegator;
    }

    @Override
    public final void run() {
        if (!delegator.tryLock(this)) {
            return;
        }

        doRun();
    }

    protected abstract void doRun();

    @Override
    public String getLockId() {
        return getName();
    }

}
