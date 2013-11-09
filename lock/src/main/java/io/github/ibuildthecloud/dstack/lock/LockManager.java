package io.github.ibuildthecloud.dstack.lock;

import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.provider.LockProvider;

public interface LockManager {

    <T,E extends Throwable> T lock(LockDefinition lockDef, LockCallbackWithException<T,E> callback, Class<E> clz) throws E;

    <T> T lock(LockDefinition lockDef, LockCallback<T> callback);

    <T> T tryLock(LockDefinition lockDef, LockCallback<T> callback);

    <T,E extends Throwable> T tryLock(LockDefinition lockDef, LockCallbackWithException<T,E> callback, Class<E> clz) throws E;

    LockProvider getLockProvider();

}
