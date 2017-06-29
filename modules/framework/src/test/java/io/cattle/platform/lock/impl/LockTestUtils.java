package io.cattle.platform.lock.impl;

import io.cattle.platform.lock.Lock;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;

import org.mockito.Mockito;

public class LockTestUtils {

    public static Lock goodLock(LockDefinition def) {
        return getLock(true, def);
    }

    public static Lock badLock(LockDefinition def) {
        return getLock(false, def);
    }

    public static Lock getLock(boolean good, LockDefinition def) {
        Lock lock = Mockito.mock(Lock.class);

        if (def != null) {
            Mockito.when(lock.getLockDefinition()).thenReturn(def);
        }

        Mockito.when(lock.tryLock()).thenReturn(good);
        if (!good) {
            Mockito.doThrow(new FailedToAcquireLockException(def)).when(lock).lock();
        }

        return lock;
    }
}
