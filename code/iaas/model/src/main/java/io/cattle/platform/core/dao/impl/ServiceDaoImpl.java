package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.RecordHandler;

public class ServiceDaoImpl extends AbstractJooqDao implements ServiceDao {

    @Inject
    ObjectManager objectManager;

    @Override
    public Service getServiceByExternalId(Long accountId, String externalId) {
        return create().selectFrom(SERVICE)
                .where(SERVICE.ACCOUNT_ID.eq(accountId))
                .and(SERVICE.REMOVED.isNull())
                .and(SERVICE.EXTERNAL_ID.eq(externalId))
                .fetchAny();
    }

    @Override
    public ServiceIndex createServiceIndex(Service service, String launchConfigName, String serviceIndex) {
        ServiceIndex serviceIndexObj = objectManager.findAny(ServiceIndex.class, SERVICE_INDEX.SERVICE_ID,
                service.getId(),
                SERVICE_INDEX.LAUNCH_CONFIG_NAME, launchConfigName, SERVICE_INDEX.SERVICE_INDEX_, serviceIndex,
                SERVICE_INDEX.REMOVED, null);
        if (serviceIndexObj == null) {
            serviceIndexObj = objectManager.create(ServiceIndex.class, SERVICE_INDEX.SERVICE_ID,
                    service.getId(),
                    SERVICE_INDEX.LAUNCH_CONFIG_NAME, launchConfigName, SERVICE_INDEX.SERVICE_INDEX_, serviceIndex,
                    SERVICE_INDEX.ACCOUNT_ID, service.getAccountId());
        }
        return serviceIndexObj;
    }

    @Override
    public Service getServiceByServiceIndexId(long serviceIndexId) {
        Record record = create()
                .select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_INDEX).on(SERVICE.ID.eq(SERVICE_INDEX.SERVICE_ID))
                .where(SERVICE_INDEX.ID.eq(serviceIndexId))
                .fetchAny();

        return record == null ? null : record.into(Service.class);
    }

    @Override
    public boolean isServiceInstance(Instance instance) {
        return instance.getDeploymentUnitUuid() != null;
    }

    @Override
    public Map<Long, List<Object>> getServicesForInstances(List<Long> ids, final IdFormatter idFormatter) {
        final Map<Long, List<Object>> result = new HashMap<>();
        create().select(SERVICE_EXPOSE_MAP.INSTANCE_ID, SERVICE_EXPOSE_MAP.SERVICE_ID)
            .from(SERVICE_EXPOSE_MAP)
            .join(SERVICE)
                .on(SERVICE.ID.eq(SERVICE_EXPOSE_MAP.SERVICE_ID))
            .where(SERVICE_EXPOSE_MAP.REMOVED.isNull()
                    .and(SERVICE.REMOVED.isNull())
                    .and(SERVICE_EXPOSE_MAP.INSTANCE_ID.in(ids)))
            .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                @Override
                public void next(Record2<Long, Long> record) {
                    Long serviceId = record.getValue(SERVICE_EXPOSE_MAP.SERVICE_ID);
                    Long instanceId = record.getValue(SERVICE_EXPOSE_MAP.INSTANCE_ID);
                    List<Object> list = result.get(instanceId);
                    if (list == null) {
                        list = new ArrayList<>();
                        result.put(instanceId, list);
                    }
                    list.add(idFormatter.formatId("service", serviceId));
                }
            });

        return result;
    }
}
