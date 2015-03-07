package io.cattle.platform.zookeeper.lock;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.task.Task;

import javax.inject.Inject;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;

public class ZookeeperCleanTask implements Task {

    private static final DynamicLongProperty DELAY = ArchaiusUtil.getLong("zookeeper.stale.lock.seconds");
    private static final Logger log = LoggerFactory.getLogger(ZookeeperCleanTask.class);

    CuratorFramework client;

    @Override
    public void run() {
        log.debug("ZooKeeper Cleanup");
        recurce(ZookeeperLockProvider.BASE_PATH.get(), System.currentTimeMillis() - DELAY.get() * 1000);
    }

    protected boolean recurce(String path, long deleteBefore) {
        boolean hasChildren = false;

        try {
            for (String child : client.getChildren().forPath(path)) {
                String childPath = path + "/" + child;

                Stat stat;
                try {
                    stat = client.checkExists().forPath(childPath);
                } catch (Exception e) {
                    continue;
                }

                if (stat == null) {
                    continue;
                }

                hasChildren |= recurce(childPath, deleteBefore);
            }
        } catch (Exception e1) {
            return true;
        }

        if (hasChildren) {
            return hasChildren;
        }

        try {
            Stat stat = client.checkExists().forPath(path);

            if (stat.getEphemeralOwner() <= 0 && stat.getNumChildren() == 0 && stat.getMtime() < deleteBefore) {
                client.delete().forPath(path);
                log.debug("Deleted ZooKeeper path [{}]", path);
                return false;
            }
        } catch (Exception e) {
        }

        return true;
    }

    @Override
    public String getName() {
        return "zookeeper.cleanup";
    }

    public CuratorFramework getClient() {
        return client;
    }

    @Inject
    public void setClient(CuratorFramework client) {
        this.client = client;
    }

}
