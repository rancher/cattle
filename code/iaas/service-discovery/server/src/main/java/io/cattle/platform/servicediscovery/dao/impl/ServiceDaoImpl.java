package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ServiceDaoImpl extends AbstractJooqDao implements ServiceDao {
    @Inject
    ObjectManager objectManager;
    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Override
    public List<? extends Service> getServicesOnHost(long hostId) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .join(INSTANCE_HOST_MAP)
                    .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .where(INSTANCE_HOST_MAP.HOST_ID.eq(hostId))
                .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                .and(SERVICE.REMOVED.isNull())
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public List<? extends Instance> getInstancesWithHealtcheckEnabled(long accountId) {
        return create().select(INSTANCE.fields())
                .from(INSTANCE)
                .join(HEALTHCHECK_INSTANCE)
                .on(HEALTHCHECK_INSTANCE.INSTANCE_ID.eq(INSTANCE.ID))
                .and(HEALTHCHECK_INSTANCE.REMOVED.isNull())
                .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STATE.in(InstanceConstants.STATE_STARTING, InstanceConstants.STATE_RUNNING)
                        .and(INSTANCE.ACCOUNT_ID.eq(accountId)))
                .fetchInto(InstanceRecord.class);
    }


    @Override
    public List<Service> getConsumingLbServices(long serviceId) {
        List<Service> lbServices = new ArrayList<>();
        findConsumingServicesImpl(serviceId, lbServices);
        return lbServices;
    }

    protected void findConsumingServicesImpl(long serviceId, List<Service> lbServices) {
        List<? extends ServiceConsumeMap> consumingServicesMaps = consumeMapDao
                .findConsumingServices(serviceId);
        for (ServiceConsumeMap consumingServiceMap : consumingServicesMaps) {
            Service consumingService = objectManager.loadResource(Service.class, consumingServiceMap.getServiceId());
            if (consumingService.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
                lbServices.add(consumingService);
            } else if (consumingService.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)) {
                if (consumingService.getId().equals(serviceId)) {
                    continue;
                }
                findConsumingServicesImpl(consumingService.getId(), lbServices);
            }
        }
    }
}
