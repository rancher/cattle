package io.cattle.platform.lock.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import io.cattle.platform.lock.Lock;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;

import org.junit.Test;

public class MultiLockTest {

    @Test
    public void test_good_lock() {
        Lock good = LockTestUtils.goodLock(null);
        Lock good2 = LockTestUtils.goodLock(null);

        MultiLock multiLock = new MultiLock(null, good, good2);
        multiLock.lock();

        verify(good, times(0)).tryLock();
        verify(good, times(1)).lock();
        verify(good2, times(0)).tryLock();
        verify(good2, times(1)).lock();
    }

    @Test
    public void test_good_tryLock() {
        Lock good = LockTestUtils.goodLock(null);
        Lock good2 = LockTestUtils.goodLock(null);

        MultiLock multiLock = new MultiLock(null, good, good2);
        multiLock.tryLock();

        verify(good, times(1)).tryLock();
        verify(good, times(0)).lock();
        verify(good2, times(1)).tryLock();
        verify(good2, times(0)).lock();
    }

    @Test
    public void test_good_unlock() {
        Lock good = LockTestUtils.goodLock(null);
        Lock good2 = LockTestUtils.goodLock(null);

        MultiLock multiLock = new MultiLock(null, good, good2);
        multiLock.unlock();

        verify(good, times(1)).unlock();
        verify(good2, times(1)).unlock();
    }

    @Test
    public void test_bad_lock() {
        Lock good = LockTestUtils.goodLock(null);
        Lock bad = LockTestUtils.badLock(null);
        Lock good2 = LockTestUtils.goodLock(null);

        try {
            MultiLock multiLock = new MultiLock(null, good, bad, good2);
            multiLock.lock();
            fail();
        } catch (FailedToAcquireLockException e) {
        }

        verify(good, times(1)).lock();
        verify(bad, times(1)).lock();
        verify(good2, times(0)).lock();
    }

    @Test
    public void test_bad_trylock() {
        Lock good = LockTestUtils.goodLock(null);
        Lock bad = LockTestUtils.badLock(null);
        Lock good2 = LockTestUtils.goodLock(null);

        MultiLock multiLock = new MultiLock(null, good, bad, good2);
        assertTrue(!multiLock.tryLock());

        verify(good, times(1)).tryLock();
        verify(bad, times(1)).tryLock();
        verify(good2, times(0)).tryLock();
    }

}
