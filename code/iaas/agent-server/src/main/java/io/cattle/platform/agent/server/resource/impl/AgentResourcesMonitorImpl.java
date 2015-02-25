package io.cattle.platform.agent.server.resource.impl;

import static io.cattle.platform.core.model.tables.PhysicalHostTable.*;
import io.cattle.platform.agent.server.ping.dao.PingDao;
import io.cattle.platform.agent.server.resource.AgentResourcesEventListener;
import io.cattle.platform.agent.server.util.AgentConnectionUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.netflix.config.DynamicLongProperty;

public class AgentResourcesMonitorImpl implements AgentResourcesEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentResourcesMonitorImpl.class);
    private static final DynamicLongProperty CACHE_RESOURCE = ArchaiusUtil.getLong("agent.resource.monitor.cache.resource.seconds");

    PingDao pingDao;
    GenericResourceDao resourceDao;
    StoragePoolDao storagePoolDao;
    IpAddressDao ipAddressDao;
    ObjectManager objectManager;
    LockDelegator lockDelegator;
    LockManager lockManager;
    Cache<String,Boolean> resourceCache;

    public AgentResourcesMonitorImpl() {
        super();
        buildCache();
        CACHE_RESOURCE.addCallback(new Runnable() {
            @Override
            public void run() {
                buildCache();
            }
        });
    }
    
    protected void buildCache() {
        resourceCache = CacheBuilder.newBuilder().expireAfterWrite(CACHE_RESOURCE.get(), TimeUnit.SECONDS)
                .build();
    }
    
    @Override
    public void pingReply(Ping ping) {
        String agentIdStr = ping.getResourceId();
        if ( agentIdStr == null ) {
            return;
        }

        long agentId = Long.parseLong(agentIdStr);
        LockDefinition lockDef = AgentConnectionUtils.getConnectionLock(agentId);
        if ( ! lockDelegator.isLocked(lockDef) ) {
            return;
        }

        if ( ping.getData() == null ) {
            return;
        }

        final AgentResources resources = processResources(ping);
        if ( ! resources.hasContent() ) {
            return;
        }

        Boolean done = resourceCache.getIfPresent(resources.getHash());

        if ( done != null && done.booleanValue() ) {
            return;
        }

        final Agent agent = objectManager.loadResource(Agent.class, agentId);

        lockManager.lock(new AgentResourceCreateLock(agent), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Boolean done = resourceCache.getIfPresent(resources.getHash());

                if ( done != null && done.booleanValue() ) {
                    return;
                }

                Map<String,Host> hosts = setHosts(agent, resources);
                setStoragePools(hosts, agent, resources);
                setIpAddresses(hosts, agent, resources);

                resourceCache.put(resources.getHash(), true);
            }
        });
    }

    protected Map<String,StoragePool> setStoragePools(Map<String,Host> hosts, Agent agent, AgentResources resources) {
        Map<String, StoragePool> pools = pingDao.getStoragePools(agent.getId());

        for ( Map.Entry<String,Map<String,Object>> poolData : resources.getStoragePools().entrySet() ) {
            String uuid = poolData.getKey();
            Map<String,Object> data = poolData.getValue();

            if ( pools.containsKey(uuid) ) {
                continue;
            }

            Host host = hosts.get(ObjectUtils.toString(data.get(HostConstants.FIELD_HOST_UUID), null));

            if ( host == null ) {
                continue;
            }

            data = createData(agent, uuid, data);
            pools.put(uuid, storagePoolDao.mapNewPool(host, data));
        }

        return pools;
    }

    protected void setIpAddresses(Map<String,Host> hosts, Agent agent, AgentResources resources) {
        for ( Map.Entry<String,Map<String,Object>> ipData : resources.getIpAddresses().entrySet() ) {
            String address = ipData.getKey();
            Map<String,Object> data = ipData.getValue();
            Host host = hosts.get(ObjectUtils.toString(data.get(HostConstants.FIELD_HOST_UUID), null));

            if ( host == null ) {
                continue;
            }

            List<IpAddress> ips = objectManager.mappedChildren(host, IpAddress.class);
            if ( ips.size() == 0 ) {
                ipAddressDao.assignAndActivateNewAddress(host, address);
            } else {
                IpAddress ip = ips.get(0);
                if ( ! address.equalsIgnoreCase(ip.getAddress()) ) {
                    ip.setAddress(address);
                    objectManager.persist(ip);
                }
            }
        }
    }

    protected Map<String,Host> setHosts(Agent agent, AgentResources resources) {
        Map<String, Host> hosts = pingDao.getHosts(agent.getId());

        for ( Map.Entry<String,Map<String,Object>> hostData : resources.getHosts().entrySet() ) {
            String uuid = hostData.getKey();
            Map<String,Object> data = hostData.getValue();
            String physicalHostUuid = ObjectUtils.toString(data.get(HostConstants.FIELD_PHYSICAL_HOST_UUID), null);
            Long physicalHostId = getPhysicalHost(agent, physicalHostUuid, new HashMap<String,Object>());

            if ( hosts.containsKey(uuid) ) {
                Host host = hosts.get(uuid);
                if ( physicalHostId != null && ! physicalHostId.equals(host.getPhysicalHostId()) ) {
                    host.setPhysicalHostId(physicalHostId);
                    objectManager.persist(host);
                }

                continue;
            }

            data = createData(agent, uuid, data);
            data.put(HostConstants.FIELD_PHYSICAL_HOST_ID, physicalHostId);

            hosts.put(uuid, resourceDao.createAndSchedule(Host.class, data));
        }

        return hosts;
    }

    protected Long getPhysicalHost(Agent agent, String uuid, Map<String,Object> properties) {
        if ( uuid == null ) {
            return null;
        }

        Map<String, PhysicalHost> hosts = pingDao.getPhysicalHosts(agent.getId());
        PhysicalHost host = hosts.get(uuid);

        if ( host != null ) {
            return host.getId();
        }

        host = objectManager.findAny(PhysicalHost.class,
                PHYSICAL_HOST.UUID, uuid);

        if ( host != null && host.getRemoved() == null ) {
            Long agentId = DataAccessor.fields(host).withKey(AgentConstants.ID_REF).as(Long.class);
            // For security purposes, ensure the agentIds match.
            if ( agentId != null && agentId.longValue() == agent.getId() ) {
                host.setAgentId(agent.getId());
                DataAccessor.fields(host).withKey(AgentConstants.ID_REF).remove();
                objectManager.persist(host);
                return host.getId();
            }
        } else if ( host == null ) {
            // For physical hosts created via the Rancher docker machine API
            // TODO Should we do a match up on account id to help prevent physical host stealing?
            // Actually, the answer to the above depends on how go-machine-service will create agents on behalf of
            // users.
            host = objectManager.findAny(PhysicalHost.class, PHYSICAL_HOST.EXTERNAL_ID, uuid);
            // For security purposes, only allow this type of assignment if the host doesn't yet have an agentId
            if ( host != null && host.getAgentId() == null && host.getRemoved() == null ) {
                host.setAgentId(agent.getId());
                DataAccessor.fields(host).withKey(AgentConstants.ID_REF).remove();
                objectManager.persist(host);
                return host.getId();
            }
        }

        Map<String,Object> data = createData(agent, uuid, properties);
        host = resourceDao.createAndSchedule(PhysicalHost.class, data);

        return host.getId();
    }

    protected Map<String,Object> createData(Agent agent, String uuid, Map<String,Object> data) {
        Map<String,Object> properties = new HashMap<>(data);
        properties.put(HostConstants.FIELD_REPORTED_UUID, uuid);
        properties.remove(ObjectMetaDataManager.UUID_FIELD);

        Long accountId = DataAccessor.fromDataFieldOf(agent)
                .withKey(AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID)
                .as(Long.class);

        if ( accountId == null ) {
            accountId = agent.getAccountId();
        }

        properties.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);
        properties.put(AgentConstants.ID_REF, agent.getId());

        return properties;
    }

    protected AgentResources processResources(Ping ping) {
        AgentResources resources = new AgentResources();
        List<Map<String,Object>> pingData = ping.getData().getResources();

        if ( pingData == null ) {
            return resources;
        }

        for ( Map<String,Object> resource : pingData ) {
            final String type = ObjectUtils.toString(resource.get(ObjectMetaDataManager.TYPE_FIELD), null);
            final String uuid = ObjectUtils.toString(resource.get(ObjectMetaDataManager.UUID_FIELD), null);

            if ( type == null || uuid == null ) {
                log.error("type [{}] or uuid [{}] is null for resource on pong from agent [{}]", type, uuid, ping.getResourceId());
                continue;
            }

            if ( type.equals(HostConstants.TYPE) ) {
                resources.getHosts().put(uuid, resource);
            } else if ( type.equals(StoragePoolConstants.TYPE) ) {
                resources.getStoragePools().put(uuid, resource);
            } else if ( type.equals(IpAddressConstants.TYPE) ) {
                resources.getIpAddresses().put(uuid, resource);
            }
        }

        return resources;
    }

    public LockDelegator getLockDelegator() {
        return lockDelegator;
    }

    @Inject
    public void setLockDelegator(LockDelegator lockDelegator) {
        this.lockDelegator = lockDelegator;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public PingDao getPingDao() {
        return pingDao;
    }

    @Inject
    public void setPingDao(PingDao pingDao) {
        this.pingDao = pingDao;
    }

    public GenericResourceDao getResourceDao() {
        return resourceDao;
    }

    @Inject
    public void setResourceDao(GenericResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    public StoragePoolDao getStoragePoolDao() {
        return storagePoolDao;
    }

    @Inject
    public void setStoragePoolDao(StoragePoolDao storagePoolDao) {
        this.storagePoolDao = storagePoolDao;
    }

    public IpAddressDao getIpAddressDao() {
        return ipAddressDao;
    }

    @Inject
    public void setIpAddressDao(IpAddressDao ipAddressDao) {
        this.ipAddressDao = ipAddressDao;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

}