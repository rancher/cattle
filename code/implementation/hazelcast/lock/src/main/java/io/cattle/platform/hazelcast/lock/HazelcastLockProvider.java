package io.cattle.platform.hazelcast.lock;

import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.provider.impl.AbstractStandardLockProvider;
import io.cattle.platform.lock.provider.impl.StandardLock;

import javax.inject.Inject;

import com.hazelcast.core.HazelcastInstance;

public class HazelcastLockProvider extends AbstractStandardLockProvider {

    HazelcastInstance hazelcast;

    public HazelcastLockProvider() {
        setReferenceCountLocks(false);
    }

    @Override
    protected StandardLock createLock(LockDefinition lockDefinition) {
        return new StandardLock(lockDefinition, hazelcast.getLock(lockDefinition.getLockId()));
    }

    @Override
    protected void destroyLock(StandardLock lock) {
        // TODO: GC locks....
        // ((ILock)lock.getLock()).destroy();
    }

    public HazelcastInstance getHazelcast() {
        return hazelcast;
    }

    @Inject
    public void setHazelcast(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

}
