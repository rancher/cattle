package io.cattle.platform.hazelcast.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.hazelcast.core.IMap;

public class HazelcastMapLock implements Lock {

    IMap<String, Object> map;
    String key;

    public HazelcastMapLock(IMap<String, Object> map, String key) {
        this.map = map;
        this.key = key;
    }

    @Override
    public void lock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        return map.tryLock(key);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return map.tryLock(key, time, unit);
    }

    @Override
    public void unlock() {
        map.unlock(key);
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

}
