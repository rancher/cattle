package io.cattle.platform.hazelcast.lock;

import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.provider.impl.AbstractStandardLockProvider;
import io.cattle.platform.lock.provider.impl.StandardLock;

import javax.inject.Inject;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class HazelcastLockProvider extends AbstractStandardLockProvider {

    private static final String LOCK_MAP = "cattle-locks";

    @Inject
    HazelcastInstance hazelcast;

    public HazelcastLockProvider() {
    }

    @Override
    protected StandardLock createLock(LockDefinition lockDefinition) {
        IMap<String, Object> map = hazelcast.getMap(LOCK_MAP);
        return new StandardLock(lockDefinition, new HazelcastMapLock(map, lockDefinition.getLockId()));
    }

    @Override
    protected void destroyLock(final StandardLock lock) {
    }

}