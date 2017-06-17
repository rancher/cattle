package io.cattle.platform.lock;

public interface LockCallbackWithException<T, E extends Throwable> {

    T doWithLock() throws E;

}
