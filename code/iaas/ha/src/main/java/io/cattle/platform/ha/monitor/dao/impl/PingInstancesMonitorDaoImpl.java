package io.cattle.platform.ha.monitor.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.ha.monitor.dao.PingInstancesMonitorDao;
import io.cattle.platform.ha.monitor.model.KnownInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PingInstancesMonitorDaoImpl extends AbstractJooqDao implements PingInstancesMonitorDao {

    @Override
    public Map<String, KnownInstance> getInstances(long agentId) {
        List<KnownInstance> instances = create()
                .select(INSTANCE.fields())
                .from(AGENT)
                .join(HOST)
                    .on(AGENT.ID.eq(HOST.AGENT_ID))
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID)
                        .and(INSTANCE_HOST_MAP.REMOVED.isNull()))
                .join(INSTANCE)
                    .on(INSTANCE.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .where(INSTANCE.REMOVED.isNull()
                    .and(AGENT.ID.eq(agentId)))
                .fetchInto(KnownInstance.class);

        Map<String, KnownInstance> result = new HashMap<>();
        for (KnownInstance i : instances)
            result.put(i.getUuid(), i);

        return result;
    }

    @Override
    public Long getAgentIdForInstanceHostMap(String instanceHostMap) {
        if ( instanceHostMap == null ) {
            return null;
        }

        return create()
                .select(HOST.AGENT_ID)
                .from(HOST)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                .where(INSTANCE_HOST_MAP.ID.eq(Long.parseLong(instanceHostMap)))
                .fetchOne(HOST.AGENT_ID);
    }

}
