package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;

import org.jooq.Record;

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
}
