package io.cattle.platform.lock;

public interface LockCallback<T> {

    public T doWithLock();

}
