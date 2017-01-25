package io.cattle.platform.simple.allocator.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.PortTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.simple.allocator.dao.QueryOptions;
import io.cattle.platform.simple.allocator.dao.SimpleAllocatorDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;

public class SimpleAllocatorDaoImpl extends AbstractJooqDao implements SimpleAllocatorDao {

    private static final List<Field<?>> hostAndPortFields;
    static {
       hostAndPortFields = new ArrayList<Field<?>>(Arrays.asList(PORT.fields()));
       hostAndPortFields.add(INSTANCE_HOST_MAP.HOST_ID);
    }

    ObjectManager objectManager;

    @Override
    public Iterator<AllocationCandidate> iteratorHosts(List<String> orderedHostUuids, List<Long> volumes, QueryOptions options) {
        List<CandidateHostInfo> hostInfos = new ArrayList<>();
        Set<Long> hostIds = new HashSet<>();
        if (orderedHostUuids == null) {
            Result<Record3<String, Long, Long>> result = getHostQuery(null, options).fetch();
            Collections.shuffle(result);

            Map<Long, CandidateHostInfo> infoMap = new HashMap<>();
            for (Record3<String, Long, Long> r : result) {
                Long hostId = r.value2();
                hostIds.add(hostId);
                CandidateHostInfo hostInfo = infoMap.get(hostId);
                if (hostInfo == null) {
                    hostInfo = new CandidateHostInfo(hostId, r.value1());
                    infoMap.put(hostId, hostInfo);
                    hostInfos.add(hostInfo);
                }
                hostInfo.getPoolIds().add(r.value3());
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

                    CandidateHostInfo hostInfo = new CandidateHostInfo(hostId, uuid);
                    hostInfo.getPoolIds().addAll(poolIds);
                    hostInfos.add(hostInfo);
                    hostIds.add(hostId);
                }
            }
        }

        return new AllocationCandidateIterator(objectManager, hostInfos, volumes);
    }

    protected SelectConditionStep<Record3<String, Long, Long>> getHostQuery(List<String> orderedHostUUIDs, QueryOptions options) {
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
                .where(getQueryOptionCondition(options, orderedHostUUIDs));
    }

    protected Condition inHostList(List<String> hostUUIDs) {
        if (hostUUIDs == null || hostUUIDs.isEmpty()) {
            return DSL.trueCondition();
        }
        return HOST.UUID.in(hostUUIDs);
    }

    protected Condition getQueryOptionCondition(QueryOptions options, List<String> orderedHostUUIDs) {
        Condition condition = null;

        if (options.getAccountId() != null) {
            condition = append(condition, HOST.ACCOUNT_ID.eq(options.getAccountId()));
        }

        if (options.getRequestedHostId() != null) {
            condition = append(condition, HOST.ID.eq(options.getRequestedHostId()));
            return condition;
        }

        condition = append(condition, AGENT.ID.isNull().or(AGENT.STATE.eq(CommonStatesConstants.ACTIVE))
        .and(HOST.STATE.in(CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE))
        .and(STORAGE_POOL.STATE.eq(CommonStatesConstants.ACTIVE))
        .and(inHostList(orderedHostUUIDs)));

        if ( options.getHosts().size() > 0 ) {
            condition = append(condition, HOST.ID.in(options.getHosts()));
        }

        if ( options.getKind() != null ) {
            condition = append(condition,
                    HOST.KIND.eq(options.getKind()).and(STORAGE_POOL.KIND.eq(options.getKind())));
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
