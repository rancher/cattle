package io.cattle.platform.lock.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import io.cattle.platform.lock.Lock;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockCallbackWithException;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.definition.MultiLockDefinition;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.lock.provider.LockProvider;

import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

public class LockManagerImplTest {

    LockProvider lockProvider;
    LockManagerImpl lockManager;
    LockDefinition goodLockDef = new TestLockDefinition("good");
    LockDefinition good2LockDef = new TestLockDefinition("good2");
    LockDefinition badLockDef = new TestLockDefinition("bad");

    Lock goodLock = LockTestUtils.goodLock(goodLockDef);
    Lock good2Lock = LockTestUtils.goodLock(good2LockDef);
    Lock badLock = LockTestUtils.badLock(badLockDef);

    @Before
    public void setUp() {
        lockProvider = mock(LockProvider.class);
        when(lockProvider.getLock(goodLockDef)).thenReturn(goodLock);
        when(lockProvider.getLock(good2LockDef)).thenReturn(good2Lock);
        when(lockProvider.getLock(badLockDef)).thenReturn(badLock);

        lockManager = new LockManagerImpl();
        lockManager.setLockProvider(lockProvider);
    }

    @Test
    public void test_bad_multilock() {
        MultiLockDefinition def = new TestMultiLockDefinition(goodLockDef, badLockDef, good2LockDef);

        try {
            lockManager.lock(def, new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    fail();
                }
            });
        } catch (FailedToAcquireLockException e) {
            assertTrue(e.isLock(badLockDef));
        }

        verify(goodLock, times(1)).lock();
        verify(badLock, times(1)).lock();
        verify(good2Lock, times(0)).lock();

        verify(goodLock, times(1)).unlock();
        verify(badLock, times(1)).unlock();
        verify(good2Lock, times(1)).unlock();

        verify(lockProvider, times(1)).getLock(goodLockDef);
        verify(lockProvider, times(1)).getLock(badLockDef);
        verify(lockProvider, times(1)).getLock(good2LockDef);

        verify(lockProvider, times(1)).releaseLock(goodLock);
        verify(lockProvider, times(1)).releaseLock(badLock);
        verify(lockProvider, times(1)).releaseLock(good2Lock);
    }

    @Test
    public void test_good_multilock() {
        MultiLockDefinition def = new TestMultiLockDefinition(goodLockDef, goodLockDef, good2LockDef);

        final AtomicInteger i = new AtomicInteger();
        lockManager.lock(def, new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                i.incrementAndGet();
            }
        });

        assertEquals(1, i.intValue());

        verify(goodLock, times(2)).lock();
        verify(good2Lock, times(1)).lock();

        verify(goodLock, times(2)).unlock();
        verify(good2Lock, times(1)).unlock();

        verify(lockProvider, times(2)).getLock(goodLockDef);
        verify(lockProvider, times(1)).getLock(good2LockDef);

        verify(lockProvider, times(2)).releaseLock(goodLock);
        verify(lockProvider, times(1)).releaseLock(good2Lock);
    }

    @Test
    public void test_exceptions() {
        try {
            lockManager.lock(goodLockDef, new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    throw new RuntimeException("42");
                }
            });
        } catch (RuntimeException e) {
            assertEquals("42", e.getMessage());
        }

        verify(goodLock, times(1)).lock();
        verify(goodLock, times(1)).unlock();
        verify(lockProvider, times(1)).getLock(goodLockDef);
        verify(lockProvider, times(1)).releaseLock(goodLock);
    }

    @Test
    public void test_checked_exceptions() {
        try {
            lockManager.lock(goodLockDef, new LockCallbackWithException<Object, FileNotFoundException>() {
                @Override
                public Object doWithLock() throws FileNotFoundException {
                    throw new FileNotFoundException("42");
                }
            }, FileNotFoundException.class);
        } catch (FileNotFoundException e) {
            assertEquals("42", e.getMessage());
        }

        verify(goodLock, times(1)).lock();
        verify(goodLock, times(1)).unlock();
        verify(lockProvider, times(1)).getLock(goodLockDef);
        verify(lockProvider, times(1)).releaseLock(goodLock);
    }

    @Test
    public void test_return() {
        assertEquals(new Long(42), lockManager.lock(goodLockDef, new LockCallback<Long>() {
            @Override
            public Long doWithLock() {
                return 42L;
            }
        }));

        verify(goodLock, times(1)).lock();
        verify(goodLock, times(1)).unlock();
        verify(lockProvider, times(1)).getLock(goodLockDef);
        verify(lockProvider, times(1)).releaseLock(goodLock);
    }

}
