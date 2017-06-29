package io.cattle.platform.ha.monitor.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.ha.monitor.dao.PingInstancesMonitorDao;
import io.cattle.platform.ha.monitor.model.KnownInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Configuration;

public class PingInstancesMonitorDaoImpl extends AbstractJooqDao implements PingInstancesMonitorDao {

    public PingInstancesMonitorDaoImpl(Configuration configuration) {
        super(configuration);
    }

    @Override
    public Map<String, KnownInstance> getInstances(long agentId) {
        List<KnownInstance> instances = create()
                .select(INSTANCE.fields())
                .from(AGENT)
                .join(HOST)
                    .on(AGENT.ID.eq(HOST.AGENT_ID))
                .join(INSTANCE)
                    .on(INSTANCE.HOST_ID.eq(HOST.ID))
                .where(INSTANCE.REMOVED.isNull()
                    .and(AGENT.ID.eq(agentId)))
                .fetchInto(KnownInstance.class);

        Map<String, KnownInstance> result = new HashMap<>();
        for (KnownInstance i : instances)
            result.put(i.getUuid(), i);

        return result;
    }

}
