package io.github.ibuildthecloud.dstack.lock;

public interface LockCallback<T> {

    public T doWithLock();

}
