package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.core.model.tables.records.StoragePoolRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.jooq.Configuration;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.RecordHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.StorageDriverTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;

public class StoragePoolDaoImpl extends AbstractJooqDao implements StoragePoolDao {

    GenericResourceDao resourceDao;
    ObjectManager objectManager;
    TransactionDelegate transaction;

    public StoragePoolDaoImpl(Configuration configuration, GenericResourceDao resourceDao, ObjectManager objectManager,
            TransactionDelegate transaction) {
        super(configuration);
        this.resourceDao = resourceDao;
        this.objectManager = objectManager;
        this.transaction = transaction;
    }

    @Override
    public StoragePool mapNewPool(Host host, Map<String, Object> properties) {
        return mapNewPool(host.getId(), properties);
    }

    @Override
    public StoragePool mapNewPool(Long hostId, Map<String, Object> properties) {
        return transaction.doInTransactionResult(() -> {
            StoragePool pool = resourceDao.createAndSchedule(StoragePool.class, properties);
            mapPoolToHost(pool.getId(), hostId);
            return pool;
        });
    }

    @Override
    public void mapPoolToHost(Long storagePoolId, Long hostId) {
        objectManager.create(StoragePoolHostMap.class,
                STORAGE_POOL_HOST_MAP.STATE, CommonStatesConstants.ACTIVE,
                STORAGE_POOL_HOST_MAP.HOST_ID, hostId,
                STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID, storagePoolId);
    }

    @Override
    public List<? extends StoragePool> findStoragePoolByDriverName(Long accountId, String driverName) {
        return create().selectFrom(STORAGE_POOL)
            .where(STORAGE_POOL.ACCOUNT_ID.eq(accountId))
            .and((STORAGE_POOL.REMOVED.isNull().or(STORAGE_POOL.STATE.eq(CommonStatesConstants.REMOVING))))
            .and(STORAGE_POOL.DRIVER_NAME.eq(driverName))
            .fetchInto(StoragePoolRecord.class);
    }

    @Override
    public Map<Long, Long> findStoragePoolHostsByDriver(Long accountId, Long storageDriverId) {
        final Map<Long, Long> result = new HashMap<>();
        create().select(HOST.ID, STORAGE_POOL.ID)
            .from(HOST)
            .leftOuterJoin(STORAGE_POOL_HOST_MAP)
                .on(STORAGE_POOL_HOST_MAP.HOST_ID.eq(HOST.ID))
            .leftOuterJoin(STORAGE_POOL)
                .on(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID)
                        .and(STORAGE_POOL.STORAGE_DRIVER_ID.eq(storageDriverId)))
            .where(STORAGE_POOL.REMOVED.isNull()
                .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull())
                .and(HOST.ACCOUNT_ID.eq(accountId))
                .and(HOST.REMOVED.isNull()))
            .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                @Override
                public void next(Record2<Long, Long> record) {
                    Long hostId = record.getValue(HOST.ID);
                    Long storagePoolId = record.getValue(STORAGE_POOL.ID);
                    if (!result.containsKey(hostId) || storagePoolId != null) {
                        result.put(hostId, storagePoolId);
                    }
                }
            });
        return result;
    }

    @Override
    public List<? extends StoragePool> findNonRemovedStoragePoolByDriver(Long storageDriverId) {
        return create().select(STORAGE_POOL.fields())
            .from(STORAGE_POOL)
            .where(STORAGE_POOL.REMOVED.isNull()
                    .and(STORAGE_POOL.STORAGE_DRIVER_ID.eq(storageDriverId)))
            .fetchInto(StoragePoolRecord.class);
    }

    @Override
    public List<Long> findVolumesInUseByServiceDriver(Long serviceId) {
        return create().select(VOLUME.ID)
            .from(VOLUME)
            .join(STORAGE_POOL)
                .on(VOLUME.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
            .join(STORAGE_DRIVER)
                .on(STORAGE_DRIVER.ID.eq(STORAGE_POOL.STORAGE_DRIVER_ID))
            .where(STORAGE_DRIVER.REMOVED.isNull()
                    .and(STORAGE_DRIVER.SERVICE_ID.eq(serviceId))
                    .and(VOLUME.REMOVED.isNull()))
            .fetch().into(Long.class);
    }

    @Override
    public StoragePool associateVolumeToPool(Long volumeId, Long storageDriverId, Long hostId) {
        Record record = create().select(STORAGE_POOL.fields())
                .from(STORAGE_POOL)
                .join(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                .where(STORAGE_POOL.STORAGE_DRIVER_ID.eq(storageDriverId)
                        .and(STORAGE_POOL_HOST_MAP.HOST_ID.eq(hostId)))
                .fetchAny();
        if (record == null) {
            return null;
        }
        StoragePool storagePool = record.into(StoragePoolRecord.class);
        create().update(VOLUME)
            .set(VOLUME.STORAGE_POOL_ID, storagePool.getId())
            .where(VOLUME.ID.eq(volumeId))
            .execute();
        return storagePool;
    }

    @Override
    public Map<Long, List<Object>> findHostsForPools(List<Long> ids, final IdFormatter idFormatter) {
        final Map<Long, List<Object>> result = new HashMap<>();

        create().select(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID, STORAGE_POOL_HOST_MAP.HOST_ID)
            .from(STORAGE_POOL_HOST_MAP)
            .where(STORAGE_POOL_HOST_MAP.REMOVED.isNull()
                    .and(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.in(ids)))
            .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                @Override
                public void next(Record2<Long, Long> record) {
                    Long hostId = record.getValue(STORAGE_POOL_HOST_MAP.HOST_ID);
                    Long storagePoolId = record.getValue(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID);
                    List<Object> pools = result.get(storagePoolId);
                    if (pools == null) {
                        pools = new ArrayList<>();
                        result.put(storagePoolId, pools);
                    }
                    pools.add(idFormatter.formatId(HostConstants.TYPE, hostId));
                }
            });
        return result;
    }

    @Override
    public Map<Long, List<Object>> findVolumesForPools(List<Long> ids, final IdFormatter idFormatter) {
        final Map<Long, List<Object>> result = new HashMap<>();

        create().select(VOLUME.STORAGE_POOL_ID, VOLUME.ID)
            .from(VOLUME)
            .join(STORAGE_POOL)
                .on(STORAGE_POOL.ID.eq(VOLUME.STORAGE_POOL_ID))
            .where(STORAGE_POOL.KIND.ne("docker")
                    .and(VOLUME.STORAGE_POOL_ID.in(ids)))
            .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                @Override
                public void next(Record2<Long, Long> record) {
                    Long volumeId = record.getValue(VOLUME.ID);
                    Long storagePoolId = record.getValue(VOLUME.STORAGE_POOL_ID);
                    List<Object> pools = result.get(storagePoolId);
                    if (pools == null) {
                        pools = new ArrayList<>();
                        result.put(storagePoolId, pools);
                    }
                    pools.add(idFormatter.formatId(VolumeConstants.TYPE, volumeId));
                }
            });
        return result;
    }

    @Override
    public List<? extends StoragePool> findNonRemovedStoragePoolByHost(long hostId) {
        return create()
                .select(STORAGE_POOL.fields())
                .from(STORAGE_POOL)
                .join(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                .where(STORAGE_POOL_HOST_MAP.HOST_ID.eq(hostId)
                        .and(STORAGE_POOL.REMOVED.isNull())
                        .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull()))
                .fetchInto(StoragePoolRecord.class);
    }

    @Override
    public List<? extends StoragePool> findPoolsForHost(long hostId) {
        return create().select(STORAGE_POOL.fields())
                .from(STORAGE_POOL)
                .join(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL.ID.eq(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID))
                .where(STORAGE_POOL.REMOVED.isNull()
                    .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull())
                    .and(STORAGE_POOL_HOST_MAP.HOST_ID.eq(hostId)))
                .fetchInto(StoragePoolRecord.class);
    }

}
