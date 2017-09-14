package io.cattle.platform.lock;

public abstract class LockCallbackNoReturn implements LockCallback<Boolean> {

    @Override
    public Boolean doWithLock() {
        doWithLockNoResult();
        return true;
    }

    public abstract void doWithLockNoResult();
}
