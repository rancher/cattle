package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.PhysicalHostTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.tables.records.AgentRecord;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.PhysicalHostRecord;
import io.cattle.platform.core.model.tables.records.StoragePoolRecord;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Record1;

@Named
public class AgentDaoImpl extends AbstractCoreDao implements AgentDao {

    @Inject
    GenericResourceDao resourceDao;
    @Inject
    ObjectManager objectManager;

    Long startTime = null;

    @Override
    public Instance getInstanceByAgent(Long agentId) {
        if (agentId == null) {
            return null;
        }

        return create()
                .selectFrom(INSTANCE)
                .where(INSTANCE.AGENT_ID.eq(agentId)
                        .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING,
                                CommonStatesConstants.REMOVING)))
                .fetchAny();
    }

    @Override
    public boolean areAllCredentialsActive(Agent agent) {
        List<Long> authedRoleAccountIds = DataAccessor.fieldLongList(agent, AgentConstants.FIELD_AUTHORIZED_ROLE_ACCOUNTS);

        if (agent.getAccountId() == null) {
            return false;
        }

        Set<Long> accountIds = new HashSet<>();
        accountIds.add(agent.getAccountId());

        for (Long aId : authedRoleAccountIds) {
            accountIds.add(aId);
        }

        List<? extends Credential> creds = create()
                .selectFrom(CREDENTIAL)
                .where(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(CREDENTIAL.ACCOUNT_ID.in(accountIds)))
                .fetch();

        Set<Long> credAccountIds = new HashSet<>();
        for (Credential cred : creds) {
            credAccountIds.add(cred.getAccountId());
        }

        return accountIds.equals(credAccountIds);
    }

    @Override
    public Agent findNonRemovedByUri(String uri) {
        return create()
                .selectFrom(AGENT)
                .where(
                        AGENT.URI.eq(uri)
                        .and(AGENT.REMOVED.isNull()))
                .fetchAny();
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

    @Override
    public String getAgentState(long agentId) {
        Record1<String> r = create().select(AGENT.STATE)
                .from(AGENT)
                .where(AGENT.ID.eq(agentId)
                        .and(AGENT.REMOVED.isNull())).fetchAny();
        return r == null ? null : r.value1();
    }

    @Override
    public List<? extends Agent> findAgentsToRemove() {
        if (startTime == null) {
            startTime = System.currentTimeMillis();
        }

        if ((System.currentTimeMillis() - startTime) <= (HostDao.HOST_REMOVE_START_DELAY.get() * 1000)) {
            return Collections.emptyList();
        }

        List<? extends Agent> agents = create().select(AGENT.fields())
                .from(AGENT)
                .leftOuterJoin(HOST)
                    .on(HOST.AGENT_ID.eq(AGENT.ID))
                .where(HOST.ID.isNull().or(HOST.REMOVED.isNotNull())
                        .and(AGENT.STATE.eq(AgentConstants.STATE_DISCONNECTED)))
                .fetchInto(AgentRecord.class);

        // This is purging old pre-1.2 agent delegates
        List<? extends Agent> oldAgents = create().select(AGENT.fields())
                .from(AGENT)
                .where(AGENT.REMOVED.isNull().and(AGENT.URI.like("delegate%")))
                .fetchInto(AgentRecord.class);

        List<Agent> result = new ArrayList<>(agents);
        result.addAll(oldAgents);

        return result;
    }
}
