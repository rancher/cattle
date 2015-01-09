package io.cattle.platform.agent.server.ping.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.PhysicalHostTable.*;
import io.cattle.platform.agent.server.group.AgentGroupManager;
import io.cattle.platform.agent.server.ping.dao.PingDao;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.PhysicalHostRecord;
import io.cattle.platform.core.model.tables.records.StoragePoolRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.util.DataAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.Condition;
import org.jooq.impl.DSL;

public class PingDaoImpl extends AbstractJooqDao implements PingDao {

    AgentGroupManager groupManager;

    @Override
    public List<? extends Agent> findAgentsToPing() {
        return create().selectFrom(AGENT).where(AGENT.STATE.eq(CommonStatesConstants.ACTIVE).and(groupCondition())).fetch();
    }

    protected Condition groupCondition() {
        if (groupManager.shouldHandleWildcard()) {
            return DSL.trueCondition();
        }

        Condition condition = DSL.falseCondition();
        Set<Long> supported = groupManager.supportedGroups();

        if (supported.size() > 0) {
            condition = AGENT.AGENT_GROUP_ID.in(supported);
        }

        if (groupManager.shouldHandleUnassigned()) {
            condition = condition.or(AGENT.AGENT_GROUP_ID.isNull());
        }

        return condition;
    }

    public AgentGroupManager getGroupManager() {
        return groupManager;
    }

    @Inject
    public void setGroupManager(AgentGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public Map<String, Host> getHosts(long agentId) {
        Map<String, Host> hosts = new HashMap<>();

        List<? extends Host> hostList = create().select(HOST.fields()).from(HOST).where(HOST.AGENT_ID.eq(agentId).and(HOST.REMOVED.isNull()))
                .fetchInto(HostRecord.class);

        for (Host host : hostList) {
            String uuid = DataAccessor.fields(host).withKey(HostConstants.FIELD_REPORTED_UUID).as(String.class);
            if (uuid == null) {
                uuid = host.getUuid();
            }

            hosts.put(uuid, host);
        }

        return hosts;
    }

    @Override
    public Map<String, StoragePool> getStoragePools(long agentId) {
        Map<String, StoragePool> pools = new HashMap<>();

        List<? extends StoragePool> poolList = create().select(STORAGE_POOL.fields()).from(STORAGE_POOL)
                .where(STORAGE_POOL.AGENT_ID.eq(agentId).and(STORAGE_POOL.REMOVED.isNull())).fetchInto(StoragePoolRecord.class);

        for (StoragePool pool : poolList) {
            String uuid = DataAccessor.fields(pool).withKey(HostConstants.FIELD_REPORTED_UUID).as(String.class);
            if (uuid == null) {
                uuid = pool.getUuid();
            }

            pools.put(uuid, pool);
        }

        return pools;
    }

    @Override
    public Map<String, PhysicalHost> getPhysicalHosts(long agentId) {
        Map<String, PhysicalHost> hosts = new HashMap<>();

        List<? extends PhysicalHost> hostList = create().select(PHYSICAL_HOST.fields()).from(PHYSICAL_HOST)
                .where(PHYSICAL_HOST.AGENT_ID.eq(agentId).and(PHYSICAL_HOST.REMOVED.isNull())).fetchInto(PhysicalHostRecord.class);

        for (PhysicalHost host : hostList) {
            String uuid = DataAccessor.fields(host).withKey(HostConstants.FIELD_REPORTED_UUID).as(String.class);
            if (uuid == null) {
                uuid = host.getUuid();
            }

            hosts.put(uuid, host);
        }

        return hosts;
    }
}
