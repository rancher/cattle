package io.cattle.platform.agent.connection.delegate.dao.impl;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import io.cattle.platform.agent.connection.delegate.dao.AgentDelegateDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import org.jooq.Record;

public class AgentDelegateDaoImpl extends AbstractJooqDao implements AgentDelegateDao {

    @Override
    public Host getHost(Agent agent) {
        Record record = create()
                .select(HOST.fields())
                    .from(INSTANCE)
                    .join(INSTANCE_HOST_MAP)
                        .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                                .and(INSTANCE_HOST_MAP.REMOVED.isNull()))
                    .join(HOST)
                        .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                    .where(INSTANCE.AGENT_ID.eq(agent.getId())
                            .and(INSTANCE.REMOVED.isNull())
                            .and(HOST.REMOVED.isNull()))
                    .fetchAny();

        return record == null ? null : record.into(Host.class);
    }

    @Override
    public Instance getInstance(Agent agent) {
        return create()
                .selectFrom(INSTANCE)
                .where(INSTANCE.AGENT_ID.eq(agent.getId())
                        .and(INSTANCE.REMOVED.isNull()))
                .fetchOne();
    }

}
