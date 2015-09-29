package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.SERVICE_CONSUME_MAP;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ServiceNicLookup extends AbstractJooqDao implements InstanceNicLookup {
    @Inject
    ObjectManager objectManager;

    @Inject
    GenericMapDao mapDao;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof Service)) {
            return null;
        }

        Service service = (Service) obj;
        List<Nic> nics = new ArrayList<>();
        populateConsumedByInstancesNics(service, nics);
        populateServiceInstancesNics(service, nics);

        return nics;
    }

    protected void populateServiceInstancesNics(Service service, List<Nic> nics) {
        // get service's instances nics
        List<? extends Nic> serviceInstancesNics = create().
                select(NIC.fields()).
                from(NIC)
                .join(INSTANCE)
                .on(INSTANCE.ID.eq(NIC.INSTANCE_ID))
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(service.getId())
                        .and(NIC.REMOVED.isNull())
                        .and(INSTANCE.REMOVED.isNull())
                        .and(SERVICE_EXPOSE_MAP.REMOVED.isNull()))
                .fetchInto(NicRecord.class);
        nics.addAll(serviceInstancesNics);
    }

    protected void populateConsumedByInstancesNics(Service service, List<Nic> nics) {
        // get the nics of the instances of the services consuming the current service
        List<? extends Nic> consumedByNics = create().
                select(NIC.fields()).
                from(NIC)
                .join(INSTANCE)
                .on(INSTANCE.ID.eq(NIC.INSTANCE_ID))
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .join(SERVICE_CONSUME_MAP)
                .on(SERVICE_CONSUME_MAP.SERVICE_ID.eq(SERVICE_EXPOSE_MAP.SERVICE_ID))
                .where(SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID.eq(service.getId())
                        .and(NIC.REMOVED.isNull())
                        .and(INSTANCE.REMOVED.isNull())
                        .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                        .and(SERVICE_CONSUME_MAP.REMOVED.isNull()))
                .fetchInto(NicRecord.class);
        nics.addAll(consumedByNics);
    }
}
