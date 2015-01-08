package io.cattle.platform.zookeeper.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.curator.framework.recipes.locks.InterProcessMutex;

public class InterProcessMutextLockWrapper implements Lock {

    String name;
    InterProcessMutex mutex;

    public InterProcessMutextLockWrapper(String name, InterProcessMutex mutex) {
        super();
        this.name = name;
        this.mutex = mutex;
    }

    @Override
    public void lock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        try {
            return tryLock(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Failed to lock [" + name + "] due to exception", e);
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        try {
            return mutex.acquire(time, unit);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to lock [" + name + "] due to exception", e);
        }
    }

    @Override
    public void unlock() {
        try {
            mutex.release();
        } catch (IllegalMonitorStateException e) {
        } catch (Exception e) {
            throw new IllegalStateException("Failed to unlock [" + name + "] due to exception", e);
        }
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

}
