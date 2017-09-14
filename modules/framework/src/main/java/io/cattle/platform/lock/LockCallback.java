package io.cattle.platform.lock;

public interface LockCallback<T> {

    T doWithLock();

}
