package io.cattle.platform.lock;

public interface LockCallbackWithException<T, E extends Throwable> {

    public T doWithLock() throws E;

}
