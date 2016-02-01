package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.DynamicSchemaTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.model.DynamicSchema;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.Arrays;
import java.util.List;

public class DynamicSchemaDaoImpl extends AbstractJooqDao implements DynamicSchemaDao {

    @Override
    public List<? extends DynamicSchema> getSchemas(long accountId) {
        return create().selectFrom(DYNAMIC_SCHEMA)
                .where(DYNAMIC_SCHEMA.ACCOUNT_ID.eq(accountId))
                .fetch();
    }

    @Override
    public DynamicSchema getSchema(String name, long accountId) {
        return create().selectFrom(DYNAMIC_SCHEMA)
                .where(DYNAMIC_SCHEMA.ACCOUNT_ID.eq(accountId)
                        .and(DYNAMIC_SCHEMA.NAME.eq(name)))
                .orderBy(DYNAMIC_SCHEMA.CREATED.asc())
                .fetchAny();
    }

    @Override
    public DynamicSchema getSchema(String name, long accountId, long serviceId) {
        return create().selectFrom(DYNAMIC_SCHEMA)
                .where(DYNAMIC_SCHEMA.ACCOUNT_ID.eq(accountId)
                        .and(DYNAMIC_SCHEMA.SERVICE_ID.eq(serviceId))
                        .and(DYNAMIC_SCHEMA.NAME.eq(name)))
                .orderBy(DYNAMIC_SCHEMA.CREATED.asc())
                .fetchAny();
    }

    @Override
    public int deleteSchemas(long serviceId) {
        return create().delete(DYNAMIC_SCHEMA)
                .where(DYNAMIC_SCHEMA.SERVICE_ID.eq(serviceId))
                .execute();
    }

    @Override
    public List<Long> getAgentForService(Service service) {
        return Arrays.asList(create().select(INSTANCE.AGENT_ID)
                .from(DYNAMIC_SCHEMA)
                .join(SERVICE)
                    .on(SERVICE.ID.eq(DYNAMIC_SCHEMA.SERVICE_ID))
                .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .join(INSTANCE)
                    .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(DYNAMIC_SCHEMA.NAME.eq(service.getKind())
                        .and(DYNAMIC_SCHEMA.ACCOUNT_ID.eq(service.getAccountId()))
                        .and(INSTANCE.AGENT_ID.isNotNull())
                        .and(SERVICE.REMOVED.isNull())
                        .and(INSTANCE.REMOVED.isNull())
                        .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                ).fetch().intoArray(INSTANCE.AGENT_ID));
    }

}
