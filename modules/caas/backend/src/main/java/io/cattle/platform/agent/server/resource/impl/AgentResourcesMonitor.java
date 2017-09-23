package io.cattle.platform.agent.server.resource.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.constants.ClusterConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Cluster;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.exception.DataChangedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.cattle.platform.object.meta.ObjectMetaDataManager.*;

public class AgentResourcesMonitor {

    private static final Logger log = LoggerFactory.getLogger(AgentResourcesMonitor.class);
    private static final DynamicLongProperty CACHE_RESOURCE = ArchaiusUtil.getLong("agent.resource.monitor.cache.resource.seconds");
    private static final String DEFAULT_UUID = "DEFAULT";

    private static final String[] UPDATABLE_HOST_FIELDS = new String[] {
            HostConstants.FIELD_HOSTNAME,
            HostConstants.FIELD_INFO,
            HostConstants.FIELD_LABELS };
    private static final Set<String> ORCHESTRATE_FIELDS = new HashSet<>(Arrays.asList(
            HostConstants.FIELD_HOSTNAME,
            HostConstants.FIELD_LABELS));

    AgentDao agentDao;
    StoragePoolDao storagePoolDao;
    GenericResourceDao resourceDao;
    ObjectManager objectManager;
    LockManager lockManager;
    EventService eventService;
    Cache<String, Boolean> resourceCache = CacheBuilder.newBuilder().expireAfterAccess(CACHE_RESOURCE.get(), TimeUnit.SECONDS).build();
    MetadataManager metadataManager;

    public AgentResourcesMonitor(AgentDao agentDao, StoragePoolDao storagePoolDao, GenericResourceDao resourceDao, ObjectManager objectManager,
            LockManager lockManager, EventService eventService, MetadataManager metadataManager) {
        super();
        this.agentDao = agentDao;
        this.storagePoolDao = storagePoolDao;
        this.resourceDao = resourceDao;
        this.objectManager = objectManager;
        this.lockManager = lockManager;
        this.eventService = eventService;
        this.metadataManager = metadataManager;
    }

    public void processPingReply(Agent agent, Ping ping) {
        final AgentResources resources = processResources(ping);
        if (!resources.hasContent()) {
            return;
        }

        String hash = resources.getHash() + "-" + agent.getId();
        Boolean done = resourceCache.getIfPresent(hash);

        if (done != null && done) {
            return;
        }

        lockManager.lock(new AgentResourceCreateLock(agent), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Boolean done = resourceCache.getIfPresent(resources.getHash());
                if (done != null && done) {
                    return;
                }

                try {
                    Map<String, Host> hosts = setHosts(agent, resources);
                    setStoragePools(hosts, agent, resources);
                    setIpAddresses(hosts, agent, resources);

                    resourceCache.put(hash, true);
                } catch (DataChangedException e) {
                    // ignore
                }
            }
        });
    }

    protected void setStoragePools(Map<String, Host> hosts, Agent agent, AgentResources resources) {
        Map<String, StoragePool> pools = agentDao.getStoragePools(agent.getId());

        for (Map.Entry<String, Map<String, Object>> poolData : resources.getStoragePools().entrySet()) {
            String uuid = poolData.getKey();
            if (DEFAULT_UUID.equals(uuid)) {
                uuid = AgentConstants.defaultUuid(agent, StoragePool.class);
            }
            Map<String, Object> data = poolData.getValue();

            if (pools.containsKey(uuid)) {
                continue;
            }

            Host host = getHostForResource(agent, data, resources, hosts);
            if (host == null) {
                continue;
            }

            data = createData(agent, uuid, data);
            pools.put(uuid, storagePoolDao.mapNewPool(host, data));
        }

    }

    protected Host getHostForResource(Agent agent, Map<String, Object> resource, AgentResources resources, Map<String, Host> hosts) {
        String uuid = getString(resource, HostConstants.FIELD_HOST_UUID);

        if (StringUtils.isBlank(uuid)) {
            if (resources.getHosts().size() != 1) {
                return null;
            }

            Map<String, Object> hostResource = resources.getHosts().values().iterator().next();
            uuid = getString(hostResource, UUID_FIELD);
            Host host = hosts.get(uuid);
            if (host != null) {
                return host;
            }

            return hosts.get(AgentConstants.defaultUuid(agent, Host.class));
        }

        return hosts.get(uuid);
    }

    private static String getString(Map<String, Object> resource, String key) {
        return DataAccessor.fromMap(resource).withKey(key).as(String.class);
    }

    protected void setIpAddresses(Map<String, Host> hosts, Agent agent, AgentResources resources) {
        for (Map.Entry<String, Map<String, Object>> ipData : resources.getIpAddresses().entrySet()) {
            String address = ipData.getKey();
            Host host = getHostForResource(agent, ipData.getValue(), resources, hosts);
            if (host == null) {
                continue;
            }

            String currentIp = DataAccessor.fieldString(host, HostConstants.FIELD_IP_ADDRESS);
            if (!Objects.equals(currentIp, address)) {
                Metadata metadata = metadataManager.getMetadataForCluster(host.getClusterId());
                host = metadata.modify(Host.class, host.getId(), obj -> objectManager.setFields(obj, HostConstants.FIELD_IP_ADDRESS, address));
                hosts.put(host.getExternalId(), host);
            }
        }
    }

    protected Map<String, Host> setHosts(Agent agent, AgentResources resources) {
        Map<String, Host> hosts = agentDao.getHosts(agent);

        for (Map.Entry<String, Map<String, Object>> hostData : resources.getHosts().entrySet()) {
            Host host = null;
            Map<Object, Object> updates = new HashMap<>();
            String uuid = hostData.getKey();
            if (DEFAULT_UUID.equals(uuid)) {
                uuid = AgentConstants.defaultUuid(agent, Host.class);
            }

            Map<String, Object> data = hostData.getValue();
            boolean orchestrate = false;
            String registrationUuid = Objects.toString(data.get(HostConstants.FIELD_MACHINE_SERVICE_REGISTRATION_UUID), null);

            if (hosts.containsKey(uuid)) {
                /* Host already exists, look for updates */
                host = hosts.get(uuid);
                for (String key : UPDATABLE_HOST_FIELDS) {
                    Object value = data.get(key);
                    if (value == null || StringUtils.isBlank(value.toString())) {
                        continue;
                    }
                    Object existingValue = io.cattle.platform.object.util.ObjectUtils.getValue(host, key);
                    if (value instanceof Map && existingValue instanceof Map) {
                        Map<Object, Object> newValueMap = new HashMap<>((Map<?, ?>)existingValue);
                        newValueMap.putAll((Map<?, ?>)value);
                        value = newValueMap;
                    }
                    if (ObjectUtils.notEqual(value, existingValue)) {
                        if (ORCHESTRATE_FIELDS.contains(key)) {
                            orchestrate = true;
                        }
                        updates.put(key, value);
                    }
                }

                if (host.getMemory() == null) {
                    Long memory = DataAccessor.fromMap(data).withKey(HostConstants.FIELD_MEMORY).as(Long.class);
                    if (memory != null) {
                        updates.put(HostConstants.FIELD_MEMORY, memory);
                    }
                } else {
                    updates.remove(HostConstants.FIELD_MEMORY);
                }

                if (host.getMilliCpu() == null) {
                    Long cpu = DataAccessor.fromMap(data).withKey(HostConstants.FIELD_MILLI_CPU).as(Long.class);
                    if (cpu != null) {
                        updates.put(HostConstants.FIELD_MILLI_CPU, cpu);
                    }
                } else {
                    updates.remove(HostConstants.FIELD_MILLI_CPU);
                }

                if (host.getLocalStorageMb() == null) {
                    Long storage = DataAccessor.fromMap(data).withKey(HostConstants.FIELD_LOCAL_STORAGE_MB).as(Long.class);
                    if (storage != null) {
                        updates.put(HostConstants.FIELD_LOCAL_STORAGE_MB, storage);
                    }
                } else {
                    updates.remove(HostConstants.FIELD_LOCAL_STORAGE_MB);
                }

                updates.put(HostConstants.FIELD_HOST_UUID, AgentConstants.defaultUuid(agent, Host.class));
                updates.put(HostConstants.FIELD_AGENT_ID, agent.getId());
            } else if (StringUtils.isBlank(registrationUuid)) {
                /* Create new */
                data = createData(agent, uuid, data);

                /* Copy createLabels to labels */
                Map<String, Object> labels = CollectionUtils.toMap(data.get(HostConstants.FIELD_LABELS));
                labels.putAll(CollectionUtils.toMap(data.get(HostConstants.FIELD_CREATE_LABELS)));
                data.put(HostConstants.FIELD_LABELS, labels);
                Cluster cluster = objectManager.loadResource(Cluster.class, agent.getClusterId());
                if (cluster != null && ClusterConstants.ORCH_KUBERNETES.equals(DataAccessor.fieldString(cluster, ClusterConstants.FIELD_ORCHESTRATION)) && !cluster.getEmbedded()) {
                    DataAccessor.setField(data, HostConstants.FIELD_IMPORTED, true);
                }
                hosts.put(uuid, resourceDao.createAndSchedule(Host.class, data));
            } else {
                /* Machine registration */
                host = objectManager.findAny(Host.class,
                        ObjectMetaDataManager.REMOVED_FIELD, null,
                        HostConstants.FIELD_AGENT_ID, null,
                        ObjectMetaDataManager.ACCOUNT_FIELD, agent.getResourceAccountId(),
                        ObjectMetaDataManager.UUID_FIELD, registrationUuid);

                if (host == null) {
                    // Didn't find existing host
                    continue;
                }

                /* Take ownership */
                Map<String, Object> labels = CollectionUtils.toMap(data.get(HostConstants.FIELD_LABELS));
                labels.putAll(CollectionUtils.toMap(data.get(HostConstants.FIELD_CREATE_LABELS)));

                updates.putAll(createData(agent, uuid, data));
                updates.put(HostConstants.FIELD_LABELS, labels);
                updates.put(HostConstants.FIELD_AGENT_ID, agent.getId());
            }

            if (updates.size() > 0) {
                Map<String, Object> updateFields = objectManager.convertToPropertiesFor(host, updates);
                if (orchestrate && !HostConstants.STATE_PROVISIONING.equals(host.getState())) {
                    host = resourceDao.updateAndSchedule(host, updateFields);
                } else {
                    Metadata metadata = metadataManager.getMetadataForAccount(agent.getResourceAccountId());
                    host = metadata.modify(Host.class, host.getId(), obj -> objectManager.setFields(obj, updateFields));
                }
                if (StringUtils.isNotBlank(host.getExternalId())) {
                    hosts.put(host.getExternalId(), host);
                }
            }
        }

        return hosts;
    }

    protected Map<String, Object> createData(Agent agent, String uuid, Map<String, Object> data) {
        Map<String, Object> properties = new HashMap<>(data);
        properties.put(HostConstants.FIELD_EXTERNAL_ID, uuid);
        properties.remove(UUID_FIELD);

        Long accountId = agent.getResourceAccountId();

        if (accountId == null) {
            accountId = agent.getAccountId();
        }

        properties.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);
        properties.put(ObjectMetaDataManager.CLUSTER_FIELD, agent.getClusterId());
        properties.put(AgentConstants.ID_REF, agent.getId());

        return properties;
    }

    protected AgentResources processResources(Ping ping) {
        AgentResources resources = new AgentResources();
        List<Map<String, Object>> pingData = ping.getData().getResources();

        if (pingData == null) {
            return resources;
        }

        for (Map<String, Object> resource : pingData) {
            String type = Objects.toString(resource.get(ObjectMetaDataManager.TYPE_FIELD), null);
            String uuid = Objects.toString(resource.get(UUID_FIELD), null);

            if (uuid == null) {
                uuid = DEFAULT_UUID;
            }

            if (type == null) {
                log.error("type [{}] is null for resource [{}] on pong from agent [{}]", null, pingData, ping.getResourceId());
                continue;
            }

            switch (type) {
                case HostConstants.TYPE:
                    resources.getHosts().put(uuid, resource);
                    break;
                case StoragePoolConstants.TYPE:
                    resources.getStoragePools().put(uuid, resource);
                    break;
                case IpAddressConstants.TYPE:
                    resources.getIpAddresses().put(uuid, resource);
                    break;
            }
        }

        return resources;
    }
}
