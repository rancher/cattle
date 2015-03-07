package io.cattle.platform.zookeeper.lock;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringProperty;

public class CuratorFactory {

    private static final DynamicStringProperty CONNNECTION = ArchaiusUtil.getString("zookeeper.connection.string");
    private static final DynamicIntProperty CONN_TIMEOUT = ArchaiusUtil.getInt("zookeeper.connection.timeout.millis");
    private static final DynamicIntProperty SESSION_TIMEOUT = ArchaiusUtil.getInt("zookeeper.session.timeout.millis");
    private static final DynamicIntProperty RETRY_TIME = ArchaiusUtil.getInt("zookeeper.sleep.between.retry.time");
    private static final DynamicIntProperty RETRIES = ArchaiusUtil.getInt("zookeeper.retries");

    private static final Logger log = LoggerFactory.getLogger(CuratorFramework.class);

    public CuratorFramework newInstance() {
        RetryPolicy retryPolicy = new RetryNTimes(RETRIES.get(), RETRY_TIME.get());
        CuratorFramework client = CuratorFrameworkFactory.newClient(CONNNECTION.get(), SESSION_TIMEOUT.get(), CONN_TIMEOUT.get(), retryPolicy);
        client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState state) {
                if (state == ConnectionState.LOST) {
                    log.error("*** Lost Connection to ZooKeeper! ***");
                    log.error("*** Lost Connection to ZooKeeper! ***");
                    log.error("*** Lost Connection to ZooKeeper! ***");
                    log.error("*** Good Bye                      ***");
                    System.exit(3);
                }
            }
        });

        client.start();

        return client;
    }
}
