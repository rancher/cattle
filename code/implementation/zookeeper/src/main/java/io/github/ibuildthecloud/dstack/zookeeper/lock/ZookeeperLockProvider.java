package io.github.ibuildthecloud.dstack.zookeeper.lock;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.lock.definition.LockDefinition;
import io.github.ibuildthecloud.dstack.lock.provider.impl.AbstractStandardLockProvider;
import io.github.ibuildthecloud.dstack.lock.provider.impl.StandardLock;

import java.util.concurrent.locks.Lock;

import javax.inject.Inject;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import com.netflix.config.DynamicStringProperty;

public class ZookeeperLockProvider extends AbstractStandardLockProvider {

    public static final DynamicStringProperty BASE_PATH = ArchaiusUtil.getString("zookeeper.lock.path");

    private static final String LOCK_DIR = "%s/%02d/%02d/%s";

    CuratorFramework client;

    @Override
    protected StandardLock createLock(LockDefinition lockDefinition) {
        return new StandardLock(lockDefinition, getLock(lockDefinition.getLockId()));
    }

    protected Lock getLock(String id) {
        int hashcode = id.hashCode();

        int first = hashcode & 0xff;
        int second = (hashcode >> 2) & 0xff;

        String path = String.format(LOCK_DIR, BASE_PATH.get(), first, second, id);

        return new InterProcessMutextLockWrapper(id, new InterProcessMutex(client, path));
    }

    public CuratorFramework getClient() {
        return client;
    }

    @Inject
    public void setClient(CuratorFramework client) {
        this.client = client;
    }

}