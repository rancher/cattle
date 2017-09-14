package io.cattle.platform.core.dao.impl;

import com.netflix.config.DynamicBooleanProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.Register;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.tables.records.AgentRecord;
import io.cattle.platform.core.model.tables.records.CredentialRecord;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.StoragePoolRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.resource.UUID;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.Record1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;

public class AgentDaoImpl extends AbstractJooqDao implements AgentDao {

    public static final DynamicBooleanProperty ALLOW_SIMULATORS = ArchaiusUtil.getBoolean("allow.simulators");

    Long startTime = null;
    ClusterDao clusterDao;
    GenericResourceDao resourceDao;
    ObjectManager objectManager;

    public AgentDaoImpl(Configuration configuration, ClusterDao clusterDao, GenericResourceDao resourceDao, ObjectManager objectManager) {
        super(configuration);
        this.clusterDao = clusterDao;
        this.resourceDao = resourceDao;
        this.objectManager = objectManager;
    }

    @Override
    public Instance getInstanceByAgent(Long agentId) {
        if (agentId == null) {
            return null;
        }

        return create()
                .selectFrom(INSTANCE)
                .where(INSTANCE.AGENT_ID.eq(agentId)
                        .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.ERROR, CommonStatesConstants.ERRORING,
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
    public Map<String, Host> getHosts(Agent agent) {
        List<? extends Host> hostList = create()
                .select(HOST.fields())
                .from(HOST)
                .where(
                        HOST.AGENT_ID.eq(agent.getId())
                        .and(HOST.REMOVED.isNull()))
                        .fetchInto(HostRecord.class);

        return groupByReportedUUid(agent, hostList);
    }

    public Map<String, Host> groupByReportedUUid(Agent agent, List<? extends Host> hostList) {
        Map<String,Host> hosts = new HashMap<>();

        for ( Host host : hostList ) {
            String uuid = host.getExternalId();
            if ( uuid == null ) {
                uuid = host.getUuid();
            }

            if (uuid != null) {
                hosts.put(uuid, host);
                if (uuid.equals(AgentConstants.defaultUuid(agent, Host.class))) {
                    hosts.put("DEFAULT", host);
                }
            }
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
            String uuid = pool.getExternalId();
            if ( uuid == null ) {
                uuid = pool.getUuid();
            }

            pools.put(uuid, pool);
        }

        return pools;
    }

    @Override
    public Agent getHostAgentForDelegate(long agentId) {
        List<? extends Agent> result = create().select(AGENT.fields())
                .from(AGENT)
                .join(HOST)
                    .on(HOST.AGENT_ID.eq(AGENT.ID))
                .join(INSTANCE)
                    .on(INSTANCE.HOST_ID.eq(HOST.ID))
                .where(INSTANCE.AGENT_ID.eq(agentId))
                .fetchInto(AgentRecord.class);
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

    @Override
    public Host getHost(Agent agent, String externalId) {
        if ("DEFAULT".equals(externalId)) {
            externalId = AgentConstants.defaultUuid(agent, Host.class);
        }
        return create()
            .select(HOST.fields())
            .from(HOST)
            .where(
                    HOST.AGENT_ID.eq(agent.getId())
                    .and(HOST.EXTERNAL_ID.eq(externalId))
                    .and(HOST.REMOVED.isNull()))
            .fetchAnyInto(HostRecord.class);
    }

    @Override
    public List<? extends Agent> findAgentsToPing() {
        Condition c = AGENT.STATE.eq(CommonStatesConstants.ACTIVE);
        for (String prefix : AgentConstants.AGENT_IGNORE_PREFIXES) {
            c = c.and(AGENT.URI.notLike(prefix + "%"));
        }
        return create()
                .select(AGENT.fields())
                .from(AGENT)
                .where(c)
                .fetchInto(AgentRecord.class);
    }

    @Override
    public Agent findAgentByExternalId(String externalId, long clusterId) {
        return create().select(AGENT.fields())
                .from(AGENT)
                .where(AGENT.CLUSTER_ID.eq(clusterId)
                        .and(AGENT.EXTERNAL_ID.eq(externalId))
                        .and(AGENT.REMOVED.isNull()))
                .fetchAnyInto(AgentRecord.class);
    }

    @Override
    public Credential findAgentCredentailByExternalId(String externalId, long clusterId) {
        return create().select(CREDENTIAL.fields())
                .from(CREDENTIAL)
                .join(AGENT)
                    .on(AGENT.ACCOUNT_ID.eq(CREDENTIAL.ACCOUNT_ID))
                .where(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE)
                    .and(AGENT.CLUSTER_ID.eq(clusterId))
                    .and(AGENT.EXTERNAL_ID.eq(externalId))
                    .and(AGENT.REMOVED.isNull()))
                .fetchAnyInto(CredentialRecord.class);
    }


    @Override
    public Agent createAgentForRegistration(Register register, long clusterId) {
        Agent agent = objectManager.findAny(Agent.class,
                AGENT.EXTERNAL_ID, register.getKey(),
                AGENT.CLUSTER_ID, clusterId,
                AGENT.REMOVED, null);

        if (agent != null) {
            return agent;
        }

        long accountId = clusterDao.getOwnerAcccountIdForCluster(clusterId);

        String format = "event://%s";
        if (register.isSimulated() && ALLOW_SIMULATORS.get()) {
            format = "sim://%s";
        }

        return resourceDao.createAndSchedule(Agent.class,
                AGENT.KIND, AccountConstants.REGISTERED_AGENT_KIND,
                AGENT.URI, String.format(format, UUID.randomUUID().toString()),
                AGENT.RESOURCE_ACCOUNT_ID, accountId,
                AGENT.CLUSTER_ID, clusterId,
                AGENT.EXTERNAL_ID, register.getKey());
    }

}
