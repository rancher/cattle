package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import io.cattle.platform.core.addon.HostSummary;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jooq.Condition;
import org.jooq.impl.DSL;

public class HostDaoImpl extends AbstractJooqDao implements HostDao {

    @Override
    public List<HostSummary> listSummaries(Long hostId, long accountId) {
        Map<Long,HostSummary> summaryMap = new TreeMap<>();

        Condition condition = HOST.ACCOUNT_ID.eq(accountId);
        if ( hostId != null ) {
            condition = condition.and(HOST.ID.eq(hostId));
        }

        List<HostSummary> summaries = create()
            .select(
                HOST.ID,
                HOST.NAME,
                HOST.DESCRIPTION,
                HOST.ACCOUNT_ID,
                HOST.STATE,
                INSTANCE.STATE.as("instance_state"),
                IP_ADDRESS.ADDRESS.as("ip_address"),
                DSL.count().as("count"))
            .from(HOST)
            .leftOuterJoin(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
            .leftOuterJoin(INSTANCE)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
            .leftOuterJoin(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.HOST_ID.eq(HOST.ID))
            .leftOuterJoin(IP_ADDRESS)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(IP_ADDRESS.ID))
            .where(condition
                    .and(HOST.REMOVED.isNull())
                    .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                    .and(INSTANCE.REMOVED.isNull())
                    .and(HOST_IP_ADDRESS_MAP.REMOVED.isNull())
                    .and(IP_ADDRESS.REMOVED.isNull())
                    .and(IP_ADDRESS.ID.isNull().or(IP_ADDRESS.ADDRESS.isNotNull())))
            .groupBy(HOST.ID, INSTANCE.STATE)
            .fetchInto(HostSummary.class);

        for ( HostSummary summary : summaries ) {
            if ( summary.getId() == null ) {
                /* Only way this is possible is if there are not hosts */
                return Collections.emptyList();
            }

            HostSummary existing = summaryMap.get(summary.getId());
            if ( existing == null ) {
                existing = new HostSummary();
                existing.setId(summary.getId());
                existing.setHostId(summary.getId());
                existing.setAccountId(summary.getAccountId());
                existing.setDescription(summary.getDescription());
                existing.setInstanceStates(new HashMap<String, Long>());
                existing.setName(summary.getName());
                existing.setState(summary.getState());
                existing.setIpAddress(summary.getIpAddress());
                summaryMap.put(existing.getId(), existing);
            }

            String instanceState = summary.getInstanceState();
            if ( instanceState != null ) {
                Map<String,Long> states = existing.getInstanceStates();
                Long existingCount = states.get(instanceState);
                existingCount = existingCount == null ? summary.getCount() : existingCount + summary.getCount();
                states.put(instanceState, existingCount);
            }
        }

        return new ArrayList<>(summaryMap.values());
    }

}
