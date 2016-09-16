package io.cattle.platform.simple.allocator.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.simple.allocator.AllocationCandidateCallback;
import io.cattle.platform.simple.allocator.dao.QueryOptions;
import io.cattle.platform.simple.allocator.dao.SimpleAllocatorDao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.Condition;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.SelectSeekStep2;
import org.jooq.impl.DSL;

import com.netflix.config.DynamicBooleanProperty;

public class SimpleAllocatorDaoImpl extends AbstractJooqDao implements SimpleAllocatorDao {

    private static final DynamicBooleanProperty SPREAD = ArchaiusUtil.getBoolean("simple.allocator.spread");

    ObjectManager objectManager;

    @Override
    public Iterator<AllocationCandidate> iteratorPools(List<Long> volumes, QueryOptions options) {
        return iteratorHosts(null, volumes, options, false, null);
    }

    @Override
    public Iterator<AllocationCandidate> iteratorHosts(List<String> orderedHostUUIDs, List<Long> volumes, QueryOptions options, AllocationCandidateCallback callback) {
        return iteratorHosts(orderedHostUUIDs, volumes, options, true, callback);
    }

    protected Iterator<AllocationCandidate> iteratorHosts(List<String> orderedHostUuids, List<Long> volumes, QueryOptions options, boolean hosts,
            AllocationCandidateCallback callback) {
        LinkedHashMap<Long, Set<Long>>orderedResults = new LinkedHashMap<>();
        Map<Long, String> hostIdsToUuids = new HashMap<>();
        if (orderedHostUuids == null) {
            Result<Record3<String, Long, Long>> result = getHostQuery(orderedHostUuids, options).fetch();

            for (Record3<String, Long, Long> r : result) {
                Long hostId = r.value2();
                hostIdsToUuids.put(hostId, r.value1());
                Set<Long> poolIDs = orderedResults.get(hostId); 
                if (poolIDs == null) {
                    poolIDs = new HashSet<Long>();
                    orderedResults.put(hostId, poolIDs);
                }
                poolIDs.add(r.value3());
            }
        } else {
            Map<String, Result<Record3<String, Long, Long>>> result = getHostQuery(orderedHostUuids, options).fetchGroups(HOST.UUID);

            for (String uuid : orderedHostUuids) {
                Result<Record3<String, Long, Long>>val = result.get(uuid);
                if (val != null) {
                    Set<Long>poolIds = new HashSet<>();
                    Long hostId = null;
                    for (Record3<String, Long, Long>r : val) {
                        poolIds.add(r.value3());
                        // host id is same in all records for this group
                        if (hostId == null) {
                            hostId = r.value2();
                        }
                    }
                    hostIdsToUuids.put(hostId, uuid);
                    orderedResults.put(hostId, poolIds);
                }
            }
        }

        return new AllocationCandidateIterator(objectManager, orderedResults, hostIdsToUuids, volumes, hosts, callback);
    }

    protected SelectSeekStep2<Record3<String, Long, Long>, Long, Long> getHostQuery(List<String> orderedHostUUIDs, QueryOptions options) {
        return create()
                .select(HOST.UUID, HOST.ID, STORAGE_POOL.ID)
                .from(HOST)
                .leftOuterJoin(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.HOST_ID.eq(HOST.ID)
                        .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull()))
                .join(STORAGE_POOL)
                    .on(STORAGE_POOL.ID.eq(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID))
                .leftOuterJoin(AGENT)
                    .on(AGENT.ID.eq(HOST.AGENT_ID))
                .where(
                    AGENT.ID.isNull().or(AGENT.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(HOST.STATE.in(CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE))
                    .and(STORAGE_POOL.STATE.eq(CommonStatesConstants.ACTIVE))
                    .and(getQueryOptionCondition(options)))
                    .and(inHostList(orderedHostUUIDs))
                .orderBy((SPREAD.get() ? HOST.COMPUTE_FREE.desc() : HOST.COMPUTE_FREE.asc()), HOST.ID.asc());
    }

    protected Condition inHostList(List<String> hostUUIDs) {
        if (hostUUIDs == null || hostUUIDs.isEmpty()) {
            return DSL.trueCondition();
        }
        return HOST.UUID.in(hostUUIDs);
    }

    protected Condition getQueryOptionCondition(QueryOptions options) {
        Condition condition = null;

        if ( options.getHosts().size() > 0 ) {
            condition = append(condition, HOST.ID.in(options.getHosts()));
        }

        if ( options.getCompute() != null ) {
            condition = append(condition, HOST.COMPUTE_FREE.ge(options.getCompute()));
        }

        if ( options.getKind() != null ) {
            condition = append(condition,
                    HOST.KIND.eq(options.getKind()).and(STORAGE_POOL.KIND.eq(options.getKind())));
        }

        if (options.getAccountId() != null) {
            condition = append(condition, HOST.ACCOUNT_ID.eq(options.getAccountId()));
        }

        return condition == null ? DSL.trueCondition() : condition;
    }

    protected Condition append(Condition base, Condition next) {
        if ( base == null ) {
            return next;
        } else {
            return base.and(next);
        }
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
