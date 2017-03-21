package io.cattle.platform.hazelcast.factory;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.hazelcast.dao.HazelcastDao;
import io.cattle.platform.hazelcast.membership.DBDiscovery;
import io.cattle.platform.hazelcast.membership.DBDiscoveryFactory;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryConfig;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class HazelcastFactory {

    private static final DynamicStringProperty NAME = ArchaiusUtil.getString("hazelcast.group.name");
    private static final DynamicStringProperty PASS = ArchaiusUtil.getString("hazelcast.group.password");

    private static final DynamicBooleanProperty JMX = ArchaiusUtil.getBoolean("hazelcast.jmx");

    private static final DynamicStringProperty LOGGING = ArchaiusUtil.getString("hazelcast.logging.type");

    private static final Logger log = LoggerFactory.getLogger(HazelcastFactory.class);

    HazelcastDao hazelcastDao;
    @Inject
    DBDiscoveryFactory dbDiscoveryFactory;
    @Inject
    DBDiscovery dbDiscovery;

    public HazelcastInstance newInstance() {
        String name = NAME.get();
        String password = PASS.get();

        if (StringUtils.isBlank(name)) {
            name = hazelcastDao.getGroupName();
        }

        if (StringUtils.isBlank(password)) {
            password = hazelcastDao.getGroupPassword();
        }

        Config config = new Config();
        if (JMX.get()) {
            config.setProperty("hazelcast.jmx", "true");
        }

        config.setProperty("hazelcast.logging.type", LOGGING.get());
        config.setProperty("hazelcast.discovery.enabled", "true");


        GroupConfig groupConfig = new GroupConfig();
        groupConfig.setName(name);
        groupConfig.setPassword(password);
        config.setGroupConfig(groupConfig);

        DiscoveryStrategyConfig dsc = new DiscoveryStrategyConfig(dbDiscoveryFactory);

        DiscoveryConfig dc = new DiscoveryConfig();
        dc.setDiscoveryStrategyConfigs(Arrays.asList(dsc));

        JoinConfig joinConfig = new JoinConfig();
        joinConfig.setDiscoveryConfig(dc);
        joinConfig.getTcpIpConfig().setEnabled(false);
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAwsConfig().setEnabled(false);

        NetworkConfig nc = new NetworkConfig();
        nc.setPort(DBDiscovery.DEFAULT_PORT.get());
        nc.setJoin(joinConfig);
        nc.setPublicAddress(dbDiscovery.getLocalConfig().getAdvertiseAddress());

        config.setNetworkConfig(nc);

        HazelcastInstance hi = Hazelcast.newHazelcastInstance(config);
        while (!hi.getPartitionService().isClusterSafe()) {
            log.info("Waiting for cluster to be in a steady state");
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Waiting for cluster to be in a steady state", e);
            }
        }

        return hi;
    }

    public HazelcastDao getHazelcastDao() {
        return hazelcastDao;
    }

    @Inject
    public void setHazelcastDao(HazelcastDao hazelcastDao) {
        this.hazelcastDao = hazelcastDao;
    }

}