package io.cattle.platform.task;

import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.lock.definition.LockDefinition;

import javax.inject.Inject;

public abstract class AbstractSingletonTask implements LockDefinition, Task {

    LockDelegator delegator;

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

    public LockDelegator getDelegator() {
        return delegator;
    }

    @Inject
    public void setDelegator(LockDelegator delegator) {
        this.delegator = delegator;
    }

}
