package io.github.ibuildthecloud.dstack.lock;

public interface LockCallbackWithException<T,E extends Throwable> {

    public T doWithLock() throws E;

}
