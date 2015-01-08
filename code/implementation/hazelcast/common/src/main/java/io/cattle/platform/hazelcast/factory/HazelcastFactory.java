package io.cattle.platform.hazelcast.factory;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.hazelcast.dao.HazelcastDao;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.hazelcast.config.AwsConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringListProperty;
import com.netflix.config.DynamicStringProperty;

public class HazelcastFactory {

    private static final DynamicStringProperty NAME = ArchaiusUtil.getString("hazelcast.group.name");
    private static final DynamicStringProperty PASS = ArchaiusUtil.getString("hazelcast.group.password");
    private static final DynamicStringListProperty IFACE = ArchaiusUtil.getList("hazelcast.network.interface");

    private static final DynamicBooleanProperty JMX = ArchaiusUtil.getBoolean("hazelcast.jmx");

    private static final DynamicBooleanProperty AWS = ArchaiusUtil.getBoolean("hazelcast.aws");
    private static final DynamicStringProperty ACCESS_KEY = ArchaiusUtil.getString("hazelcast.aws.access.key");
    private static final DynamicStringProperty SECRET_KEY = ArchaiusUtil.getString("hazelcast.aws.secret.key");
    private static final DynamicStringProperty REGION = ArchaiusUtil.getString("hazelcast.aws.region");
    private static final DynamicStringProperty HOST_HEADER = ArchaiusUtil.getString("hazelcast.aws.host.header");
    private static final DynamicStringProperty SECURITY_GROUP = ArchaiusUtil.getString("hazelcast.aws.security.group");
    private static final DynamicStringProperty TAG_KEY = ArchaiusUtil.getString("hazelcast.aws.tag.key");
    private static final DynamicStringProperty TAG_VALUE = ArchaiusUtil.getString("hazelcast.aws.tag.value");
    private static final DynamicIntProperty TIMEOUT = ArchaiusUtil.getInt("hazelcast.aws.tag.value");
    private static final DynamicStringProperty LOGGING = ArchaiusUtil.getString("hazelcast.logging.type");

    HazelcastDao hazelcastDao;

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

        GroupConfig groupConfig = new GroupConfig();
        groupConfig.setName(name);
        groupConfig.setPassword(password);
        config.setGroupConfig(groupConfig);

        if (AWS.get()) {
            AwsConfig awsConfig = new AwsConfig();
            awsConfig.setAccessKey(ACCESS_KEY.get());
            awsConfig.setSecretKey(SECRET_KEY.get());
            awsConfig.setEnabled(true);

            if (!StringUtils.isBlank(HOST_HEADER.get())) {
                awsConfig.setHostHeader(HOST_HEADER.get());
            }

            if (!StringUtils.isBlank(SECURITY_GROUP.get())) {
                awsConfig.setSecurityGroupName(SECURITY_GROUP.get());
            }

            if (!StringUtils.isBlank(TAG_KEY.get())) {
                awsConfig.setTagKey(TAG_KEY.get());
            }

            if (!StringUtils.isBlank(TAG_VALUE.get())) {
                awsConfig.setTagValue(TAG_VALUE.get());
            }

            if (!StringUtils.isBlank(REGION.get())) {
                awsConfig.setRegion(REGION.get());
            }

            if (TIMEOUT.get() > 0) {
                awsConfig.setConnectionTimeoutSeconds(TIMEOUT.get());
            }

            JoinConfig joinConfig = new JoinConfig();
            joinConfig.setAwsConfig(awsConfig);

            MulticastConfig mcast = new MulticastConfig();
            mcast.setEnabled(false);
            TcpIpConfig tcpIpConfig = new TcpIpConfig();
            tcpIpConfig.setEnabled(false);

            joinConfig.setMulticastConfig(mcast);
            joinConfig.setTcpIpConfig(tcpIpConfig);

            NetworkConfig networkConfig = new NetworkConfig();
            networkConfig.setJoin(joinConfig);

            config.setNetworkConfig(networkConfig);
        } else if (IFACE.get().size() > 0) {
            InterfacesConfig ifacesConfig = new InterfacesConfig();
            ifacesConfig.setEnabled(true);
            for (String iface : IFACE.get()) {
                ifacesConfig.addInterface(iface);
            }

            NetworkConfig networkConfig = new NetworkConfig();
            networkConfig.setInterfaces(ifacesConfig);

            config.setNetworkConfig(networkConfig);
        }

        return Hazelcast.newHazelcastInstance(config);
    }

    public HazelcastDao getHazelcastDao() {
        return hazelcastDao;
    }

    @Inject
    public void setHazelcastDao(HazelcastDao hazelcastDao) {
        this.hazelcastDao = hazelcastDao;
    }

}
