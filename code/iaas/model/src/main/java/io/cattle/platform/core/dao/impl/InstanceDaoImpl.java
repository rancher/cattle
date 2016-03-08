package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.STORAGE_POOL_HOST_MAP;
import static io.cattle.platform.core.model.tables.StoragePoolTable.STORAGE_POOL;
import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.VOLUME_STORAGE_POOL_MAP;
import static io.cattle.platform.core.model.tables.VolumeTable.VOLUME;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;

public class InstanceDaoImpl extends AbstractJooqDao implements InstanceDao {
    @Inject
    GenericMapDao mapDao;

    @Override
    public boolean isOnSharedStorage(Instance instance) {
        List<Long> poolIds = create()
                .select(STORAGE_POOL.ID)
                .from(STORAGE_POOL)
                .join(VOLUME_STORAGE_POOL_MAP)
                .on(VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                .join(VOLUME)
                .on(VOLUME_STORAGE_POOL_MAP.VOLUME_ID.eq(VOLUME.ID))
                .where(VOLUME.INSTANCE_ID.eq(instance.getId())
                        .and(VOLUME_STORAGE_POOL_MAP.REMOVED.isNull())
                        .and(VOLUME.REMOVED.isNull()))
                        .fetchInto(Long.class);

        if (poolIds.size() == 0) {
            return false;
        }

        for ( Long poolId : poolIds ) {
            List<?> result = create()
                    .select()
                    .from(STORAGE_POOL_HOST_MAP)
                    .where(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(poolId)
                            .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull()))
                            .limit(2)
                            .fetch();

            if ( result.size() <= 1 ) {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<? extends Instance> getNonRemovedInstanceOn(Long hostId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(hostId)
                            .and(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID)))
                .where(INSTANCE.REMOVED.isNull().and(
                        INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING,
                                CommonStatesConstants.REMOVING)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public Instance getInstanceByUuidOrExternalId(Long accountId, String uuid, String externalId) {
        Instance instance = null;
        Condition condition = INSTANCE.ACCOUNT_ID.eq(accountId).and(INSTANCE.STATE.notIn(CommonStatesConstants.PURGED,
                CommonStatesConstants.PURGING));

        if(StringUtils.isNotEmpty(uuid)) {
            instance = create()
                    .selectFrom(INSTANCE)
                    .where(condition
                    .and(INSTANCE.UUID.eq(uuid)))
                    .fetchAny();
        }

        if (instance == null && StringUtils.isNotEmpty(externalId)) {
            instance = create()
                    .selectFrom(INSTANCE)
                    .where(condition
                    .and(INSTANCE.EXTERNAL_ID.eq(externalId)))
                    .fetchAny();
        }

        return instance;
    }

    @Override
    public List<? extends Service> findServicesFor(Instance instance) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .where(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(instance.getId()))
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public List<? extends Instance> listNonRemovedInstances(Account account, boolean forService) {
        List<? extends Instance> serviceInstances = create().select(INSTANCE.fields())
                    .from(INSTANCE)
                    .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(INSTANCE.ACCOUNT_ID.eq(account.getId()))
                .and(INSTANCE.REMOVED.isNull())
                    .fetchInto(InstanceRecord.class);
        if (forService) {
            return serviceInstances;
        }
        List<? extends Instance> allInstances = create().select(INSTANCE.fields())
                .from(INSTANCE)
                .where(INSTANCE.ACCOUNT_ID.eq(account.getId()))
                .and(INSTANCE.REMOVED.isNull())
                .fetchInto(InstanceRecord.class);

        allInstances.removeAll(serviceInstances);
        return allInstances;
    }

    @Override
    public List<? extends Instance> findInstancesFor(Service service) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(service.getId()))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.PURGING, CommonStatesConstants.PURGED,
                                CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> findInstanceByServiceName(long accountId, String serviceName) {
        return create().select(INSTANCE.fields())
            .from(INSTANCE)
            .join(SERVICE_EXPOSE_MAP)
                .on(INSTANCE.ID.eq(SERVICE_EXPOSE_MAP.INSTANCE_ID))
            .join(SERVICE)
                .on(SERVICE.ID.eq(SERVICE_EXPOSE_MAP.SERVICE_ID))
            .where(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING)
                    .and(INSTANCE.ACCOUNT_ID.eq(accountId))
                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                    .and(SERVICE.REMOVED.isNull())
                    .and(SERVICE.NAME.eq(serviceName)))
            .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> findInstanceByServiceName(long accountId, String serviceName, String environmentName) {
        return create().select(INSTANCE.fields())
            .from(INSTANCE)
            .join(SERVICE_EXPOSE_MAP)
                .on(INSTANCE.ID.eq(SERVICE_EXPOSE_MAP.INSTANCE_ID))
            .join(SERVICE)
                .on(SERVICE.ID.eq(SERVICE_EXPOSE_MAP.SERVICE_ID))
            .join(ENVIRONMENT)
                .on(ENVIRONMENT.ID.eq(SERVICE.ENVIRONMENT_ID))
            .where(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING)
                    .and(INSTANCE.ACCOUNT_ID.eq(accountId))
                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                    .and(SERVICE.REMOVED.isNull())
                    .and(SERVICE.NAME.eq(serviceName))
                    .and(ENVIRONMENT.NAME.eq(environmentName)))
            .fetchInto(InstanceRecord.class);
    }

}
