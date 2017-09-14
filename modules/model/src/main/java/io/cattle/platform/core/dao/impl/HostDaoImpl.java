package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.RecordHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;


public class HostDaoImpl extends AbstractJooqDao implements HostDao {

    Long startTime;

    public HostDaoImpl(Configuration configuration) {
        super(configuration);
    }

    @Override
    public Map<Long, List<Object>> getInstancesPerHost(List<Long> hosts, final IdFormatter idFormatter) {
        final Map<Long, List<Object>> result = new HashMap<>();
        create().select(INSTANCE.ID, INSTANCE.HOST_ID)
            .from(INSTANCE)
            .where(INSTANCE.HOST_ID.in(hosts)
                    .and(INSTANCE.REMOVED.isNull()))
            .fetchInto((RecordHandler<Record2<Long, Long>>) record -> {
                Long hostId = record.getValue(INSTANCE.HOST_ID);
                Long instanceId = record.getValue(INSTANCE.ID);
                List<Object> list = result.get(hostId);
                if (list == null) {
                    list = new ArrayList<>();
                    result.put(hostId, list);
                }
                list.add(idFormatter.formatId(InstanceConstants.TYPE, instanceId));
            });

        return result;
    }

    @Override
    public List<? extends Host> findHostsRemove() {
        if (startTime == null) {
            startTime = System.currentTimeMillis();
        }

        if ((System.currentTimeMillis() - startTime) <= (HOST_REMOVE_START_DELAY.get() * 1000)) {
            return Collections.emptyList();
        }

        return create().select(HOST.fields())
                .from(HOST)
                .join(AGENT)
                    .on(AGENT.ID.eq(HOST.AGENT_ID))
                .where(AGENT.STATE.eq(AgentConstants.STATE_DISCONNECTED)
                        .and(HOST.STATE.in(CommonStatesConstants.ACTIVE, CommonStatesConstants.INACTIVE))
                        .and(HOST.REMOVE_AFTER.lt(new Date())))
                .fetchInto(HostRecord.class);
    }

}
