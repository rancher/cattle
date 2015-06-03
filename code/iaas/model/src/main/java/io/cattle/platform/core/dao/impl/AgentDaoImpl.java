package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.PhysicalHostTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.tables.records.AgentRecord;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.PhysicalHostRecord;
import io.cattle.platform.core.model.tables.records.StoragePoolRecord;
import io.cattle.platform.object.util.DataAccessor;

public class AgentDaoImpl extends AbstractCoreDao implements AgentDao {

    @Override
    public Agent findNonRemovedByUri(String uri) {
        return create()
                .selectFrom(AGENT)
                .where(
                        AGENT.URI.eq(uri)
                        .and(AGENT.REMOVED.isNull()))
                .fetchOne();
    }

    @Override
    public Map<String, Host> getHosts(long agentId) {
        List<? extends Host> hostList = create()
                .select(HOST.fields())
                .from(HOST)
                .where(
                        HOST.AGENT_ID.eq(agentId)
                        .and(HOST.REMOVED.isNull()))
                        .fetchInto(HostRecord.class);

        return groupByReportedUUid(hostList);
    }

    public Map<String, Host> groupByReportedUUid(List<? extends Host> hostList) {
        Map<String,Host> hosts = new HashMap<>();

        for ( Host host : hostList ) {
            String uuid = DataAccessor.fields(host).withKey(HostConstants.FIELD_REPORTED_UUID).as(String.class);
            if ( uuid == null ) {
                uuid = host.getUuid();
            }

            hosts.put(uuid, host);
        }

        return hosts;
    }

    @Override
    public Map<String, Host> getHostsForDelegate(long agentId) {
        List<? extends Host> hostList = create()
                .select(HOST.fields())
                .from(HOST)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                .join(INSTANCE)
                    .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(
                        INSTANCE.AGENT_ID.eq(agentId)
                                .and(HOST.REMOVED.isNull()))
                .fetchInto(HostRecord.class);

        return groupByReportedUUid(hostList);
    }

    @Override
    public Map<String, StoragePool> getStoragePools(long agentId) {
        Map<String,StoragePool> pools = new HashMap<>();

        List<? extends StoragePool> poolList = create()
                .select(STORAGE_POOL.fields())
                .from(STORAGE_POOL)
                .where(
                        STORAGE_POOL.AGENT_ID.eq(agentId)
                        .and(STORAGE_POOL.REMOVED.isNull()))
                        .fetchInto(StoragePoolRecord.class);

        for ( StoragePool pool : poolList ) {
            String uuid = DataAccessor.fields(pool).withKey(HostConstants.FIELD_REPORTED_UUID).as(String.class);
            if ( uuid == null ) {
                uuid = pool.getUuid();
            }

            pools.put(uuid, pool);
        }

        return pools;
    }

    @Override
    public Map<String, PhysicalHost> getPhysicalHosts(long agentId) {
        Map<String,PhysicalHost> hosts = new HashMap<>();

        List<? extends PhysicalHost> hostList = create()
                .select(PHYSICAL_HOST.fields())
                .from(PHYSICAL_HOST)
                .where(
                        PHYSICAL_HOST.AGENT_ID.eq(agentId)
                        .and(PHYSICAL_HOST.REMOVED.isNull()))
                        .fetchInto(PhysicalHostRecord.class);

        for ( PhysicalHost host : hostList ) {
            String uuid = host.getExternalId();

            if (StringUtils.isEmpty(uuid)) {
                uuid = DataAccessor.fields(host).withKey(HostConstants.FIELD_REPORTED_UUID).as(String.class);
            }

            if (StringUtils.isEmpty(uuid)) {
                uuid = host.getUuid();
            }

            hosts.put(uuid, host);
        }

        return hosts;
    }

    @Override
    public Agent getHostAgentForDelegate(long agentId) {
        List<? extends Agent> result = create().select(AGENT.fields())
                .from(AGENT)
                .join(HOST)
                    .on(HOST.AGENT_ID.eq(AGENT.ID))
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                .join(INSTANCE)
                    .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(INSTANCE.AGENT_ID.eq(agentId)).fetchInto(AgentRecord.class);
        return result.size() == 0 ? null : result.get(0);
    }
}
