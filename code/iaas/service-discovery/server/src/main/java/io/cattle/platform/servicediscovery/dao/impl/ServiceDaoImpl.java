package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;

import java.util.List;

public class ServiceDaoImpl extends AbstractJooqDao implements ServiceDao {

    @Override
    public List<? extends Service> findServicesFor(Instance instance) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .where(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(instance.getId()))
                .fetchInto(ServiceRecord.class);
    }

}
