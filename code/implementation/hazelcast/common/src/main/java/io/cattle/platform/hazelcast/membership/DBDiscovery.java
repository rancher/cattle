package io.cattle.platform.hazelcast.membership;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.ClusterMembership;
import io.cattle.platform.hazelcast.membership.dao.ClusterMembershipDAO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.hazelcast.spi.partitiongroup.PartitionGroupStrategy;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringProperty;

public class DBDiscovery extends NoExceptionRunnable implements DiscoveryStrategy, ClusterService {

    public static final DynamicStringProperty ADVERTISE_ADDRESS = ArchaiusUtil.getString("cluster.advertise.address");
    public static final DynamicStringProperty HTTP_PORT = ArchaiusUtil.getString("cluster.advertise.http.port");

    public static final DynamicStringProperty DEFAULT_HTTP_PORT = ArchaiusUtil.getString("cluster.default.http.port");
    public static final DynamicIntProperty DEFAULT_PORT = ArchaiusUtil.getInt("cluster.default.port");
    public static final DynamicIntProperty INTERVAL = ArchaiusUtil.getInt("cluster.checkin.seconds");
    public static final DynamicIntProperty MISSED_COUNT = ArchaiusUtil.getInt("cluster.checkin.misses");
    public static final DynamicStringProperty ID_FILE = ArchaiusUtil.getString("cluster.id.file");

    private static final DynamicBooleanProperty CLUSTERED = ArchaiusUtil.getBoolean("module.profile.hazelcast.eventing");

    private static final Logger log = LoggerFactory.getLogger("ConsoleStatus");

    @Inject
    ClusterMembershipDAO clusterMembershipDao;
    @Inject
    ObjectManager objectManager;
    @Inject
    JsonMapper jsonMapper;
    ScheduledExecutorService executorService;
    DiscoveryNode selfNode;

    Map<String, DiscoveryNode> hzNodes = new TreeMap<>();
    Map<Long, ClusteredMember> members = new TreeMap<>();
    String uuid = UUID.randomUUID().toString();
    ScheduledFuture<?> future;
    Map<Long, Checkin> heartbeats = new HashMap<>();
    boolean initial = true;
    boolean logStartup = true;

    @PostConstruct
    public void init() {
        try {
            readId();
            checkin();
        } catch (Exception e) {
            log.error("Failed to initialize discovery", e);
            throw new IllegalStateException(e);
        }
        initial = false;
        executorService = Executors.newSingleThreadScheduledExecutor();
        future = executorService.scheduleWithFixedDelay(this, INTERVAL.get(), INTERVAL.get(), TimeUnit.SECONDS);
        log.info("Checking cluster state on start-up");
        waitReady();
    }

    protected void readId() throws IOException {
        String file = ID_FILE.get();
        if (StringUtils.isBlank(file)) {
            return;
        }

        File f = new File(file);
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                uuid = IOUtils.toString(fis).trim();
            }
        } else {
            try (FileOutputStream fos = new FileOutputStream(f)) {
                IOUtils.write(uuid, fos, "UTF-8");
            }
        }
    }

    @Override
    public void start() {
    }

    @Override
    public synchronized Iterable<DiscoveryNode> discoverNodes() {
        return hzNodes.values();
    }

    @Override
    public void destroy() {
        future.cancel(true);
    }

    @Override
    public Map<String, Object> discoverLocalMetadata() {
        return new HashMap<>();
    }

    protected String getAddress(String address) {
        if (StringUtils.isBlank(address)) {
            address = "127.0.0.1";
        }
        if (!address.contains(":")) {
            return String.format("%s:%d", address, DEFAULT_PORT.get());
        }
        return address;
    }

    protected String getLocalAddress() {
        String address = ADVERTISE_ADDRESS.get();
        return getAddress(address);
    }

    public ClusterConfig getLocalConfig() {
        ClusterConfig config = new ClusterConfig();
        config.setClustered(CLUSTERED.get());
        config.setAdvertiseAddress(getLocalAddress());
        if (StringUtils.isBlank(HTTP_PORT.get())) {
            config.setHttpPort(Integer.parseInt(DEFAULT_HTTP_PORT.get()));
        } else {
            config.setHttpPort(Integer.parseInt(HTTP_PORT.get()));
        }
        return config;
    }

    @Override
    protected synchronized void doRun() throws Exception {
        try {
            checkin();
        } catch (Throwable t ) {
            log.error("Check-in failed", t);
            System.err.println("FATAL: Exiting due to failed cluster check-in");
            System.exit(1);
        }
    }

    public synchronized void checkin() throws Exception {
        Map<Long, ClusterMembership> activeSet = new TreeMap<>();
        ClusterConfig config = getLocalConfig();
        clusterMembershipDao.checkin(uuid, config, initial);

        for (ClusterMembership member : clusterMembershipDao.listMembers()) {
            Checkin checkin = heartbeats.get(member.getId());
            if (checkin == null) {
                checkin = new Checkin();
            }

            if (member.getHeartbeat().equals(checkin.heartbeat)) {
                checkin.count += 1;
            } else {
                checkin.count = 0;
            }

            if (checkin.count > MISSED_COUNT.get()) {
                clusterMembershipDao.delete(member);
            } else {
                activeSet.put(member.getId(), member);
            }

            checkin.heartbeat = member.getHeartbeat();
            heartbeats.put(member.getId(), checkin);
        }

        Iterator<Long> keyIter = heartbeats.keySet().iterator();
        while (keyIter.hasNext()) {
            Long key = keyIter.next();
            if (!activeSet.containsKey(key)) {
               keyIter.remove();
            }
        }

        setupHz(activeSet, config);
        setupMembers(activeSet, config);

        this.notifyAll();
    }

    protected void setupMembers(Map<Long, ClusterMembership> activeSet, ClusterConfig config) throws IOException {
        Map<Long, ClusteredMember> members = new TreeMap<>();

        for (Map.Entry<Long, ClusterMembership> entry : activeSet.entrySet()) {
            Long id = entry.getKey();
            ClusterMembership member = entry.getValue();
            ClusterConfig memberConfig = jsonMapper.readValue(member.getConfig(), ClusterConfig.class);
            members.put(id, new ClusteredMember(id, memberConfig, uuid.equals(member.getUuid()), member.getClustered()));
        }

        this.members = members;
    }

    protected void setupHz(Map<Long, ClusterMembership> activeSet, ClusterConfig selfConfig) {
        Map<String, DiscoveryNode> newNodes = new TreeMap<>();
        for (ClusterMembership member : activeSet.values()) {
            try {
                ClusterConfig config = jsonMapper.readValue(member.getConfig(), ClusterConfig.class);
                String address = getAddress(config.getAdvertiseAddress());
                String[] parts = address.split(":");

                newNodes.put(address, new SimpleDiscoveryNode(new Address(parts[0], Integer.parseInt(parts[1]))));
            } catch (Exception e) {
                log.error("Failed to register cluster member {} {}", member.getUuid(), member.getConfig());
            }
        }

        if (selfNode != null) {
            newNodes.put(selfConfig.getAdvertiseAddress(), selfNode);
        }

        if (!this.hzNodes.keySet().equals(newNodes.keySet())) {
            log.info("Cluster membership changed {}", newNodes.keySet());
        }

        this.hzNodes = newNodes;
    }

    @Override
    public PartitionGroupStrategy getPartitionGroupStrategy() {
        return null;
    }

    private static class Checkin {
        long heartbeat;
        long count;
    }

    public DiscoveryNode getSelfNode() {
        return selfNode;
    }

    public void setSelfNode(DiscoveryNode selfNode) {
        this.selfNode = selfNode;
    }

    @Override
    public synchronized boolean isMaster() {
        if (members.size() == 0) {
            return false;
        }

        return members.values().iterator().next().isSelf();
    }

    @Override
    public boolean waitReady() {
        while (!isReady()) {
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        return true;
    }

    protected synchronized boolean isReady() {
        if (isMaster()) {
            return true;
        }

        ClusterConfig local = getLocalConfig();
        if (!local.isClustered()) {
            if (logStartup) {
                log.info("Waiting to become master");
                logStartup = false;
            }
            // Non-clustered only are ready when they are master;
            return false;
        }

        ClusteredMember master = getMaster();

        if (master == null) {
            if (logStartup) {
                log.info("Waiting for a master");
            }
            return false;
        }

        // Clustered are not ready until master is clustered also
        if (master.isClustered()) {
            String[] parts = master.getConfig().getAdvertiseAddress().split(":");
            try (
                Socket socket = new Socket();
            ) {
                socket.connect(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])), 5000);
                log.info("Connection test to master");
            } catch (IOException e) {
                log.error("Failed to connect to master at {}", master.getAdvertiseAddress());
                return false;
            }
            return true;
        }

        if (logStartup) {
            log.info("Waiting for non-clustered master to die");
            logStartup = false;
        }
        return false;
    }

    @Override
    public synchronized ClusteredMember getMaster() {
        if (members.size() == 0) {
            return null;
        }

        return members.values().iterator().next();
    }

}
