package io.cattle.platform.hazelcast.lock;

import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.provider.impl.AbstractStandardLockProvider;
import io.cattle.platform.lock.provider.impl.StandardLock;
import io.cattle.platform.task.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

public class HazelcastLockProvider extends AbstractStandardLockProvider implements Task {

    HazelcastInstance hazelcast;
    Set<String> destroy = Collections.synchronizedSet(new HashSet<String>());
    Set<String> toDestroy;

    public HazelcastLockProvider() {
    }

    @Override
    protected StandardLock createLock(LockDefinition lockDefinition) {
        return new StandardLock(lockDefinition, hazelcast.getLock(lockDefinition.getLockId()));
    }

    @Override
    protected void destroyLock(final StandardLock lock) {
        if (lock.wasAcquired() && StringUtils.containsAny(lock.getLockDefinition().getLockId(), "0123456789")) {
            destroy.add(lock.getLockDefinition().getLockId());
        }
    }

    public HazelcastInstance getHazelcast() {
        return hazelcast;
    }

    @Inject
    public void setHazelcast(HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    @Override
    public String getName() {
        return "hazelcast.lock";
    }

    @Override
    public void run() {
        toDestroy = destroy;
        destroy = Collections.synchronizedSet(new HashSet<String>());

        for (String id : toDestroy) {
            ILock lock = hazelcast.getLock(id);
            try {
                if (lock.tryLock()) {
                    lock.unlock();
                    lock.destroy();
                }
            } catch (Throwable t) {
                // ignore
            }
        }
    }

}