package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.addon.MountEntry;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.tables.VolumeTable;
import io.cattle.platform.core.model.tables.records.VolumeRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Configuration;
import org.jooq.Record;
import org.jooq.Record6;
import org.jooq.RecordHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.MountTable.*;
import static io.cattle.platform.core.model.tables.StorageDriverTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolHostMapTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;

public class VolumeDaoImpl extends AbstractJooqDao implements VolumeDao {

    private static final Set<String> LOCAL_POOL_KINDS = new HashSet<>(Arrays.asList("docker", "sim"));

    GenericResourceDao resourceDao;
    ObjectManager objectManager;
    TransactionDelegate transaction;


    public VolumeDaoImpl(Configuration configuration, GenericResourceDao resourceDao, ObjectManager objectManager, TransactionDelegate transaction) {
        super(configuration);
        this.resourceDao = resourceDao;
        this.objectManager = objectManager;
        this.transaction = transaction;
    }

    @Override
    public Volume getVolumeInPoolByExternalId(String externalId, StoragePool storagePool) {
        if (StringUtils.isEmpty(externalId) || storagePool == null )
            throw new IllegalArgumentException("External ID and storage pool must have values.");

        List<VolumeRecord> volumes = create()
                .select(VolumeTable.VOLUME.fields())
                    .from(VOLUME)
                    .where(VOLUME.STORAGE_POOL_ID.eq(storagePool.getId())
                        .and(VOLUME.REMOVED.isNull())
                        .and(VOLUME.STATE.ne(CommonStatesConstants.REMOVING))
                        .and(VOLUME.EXTERNAL_ID.eq(externalId).or(VOLUME.NAME.eq(externalId))))
                .fetchInto(VolumeRecord.class);

        if ( volumes.isEmpty() )
            return null;
        else if ( volumes.size() == 1 )
            return volumes.get(0);
        else
            throw new IllegalStateException(String.format(
                    "More than one volume exists for name/externalId [%s] and storage pool [%s].", externalId, storagePool.getId()));
    }

    @Override
    public Volume createVolumeInPool(Long accountId, String name, String externalId, String driver, StoragePool storagePool, boolean isNative) {
        return transaction.doInTransactionResult(() -> {
            Volume volume = getVolumeInPoolByExternalId(externalId, storagePool);
            if (volume != null) {
                return volume;
            }

            volume = objectManager.create(Volume.class,
                    VOLUME.ACCOUNT_ID, accountId,
                    VOLUME.NAME, name,
                    VOLUME.STORAGE_POOL_ID, storagePool.getId(),
                    VOLUME.EXTERNAL_ID, externalId,
                    VolumeConstants.FIELD_DOCKER_IS_NATIVE, isNative,
                    VolumeConstants.FIELD_VOLUME_DRIVER, driver);

            return volume;
        });
    }


    @Override
    public Volume findVolumeByExternalId(Long storagePoolId, String externalId) {
        Record record = create()
            .select(VOLUME.fields())
            .from(VOLUME)
            .where(VOLUME.STORAGE_POOL_ID.eq(storagePoolId)
                    .and(VOLUME.EXTERNAL_ID.eq(externalId).or(externalId))
            .and((VOLUME.REMOVED.isNull().or(VOLUME.STATE.eq(CommonStatesConstants.REMOVING)))))
            .fetchAny();

        return record == null ? null : record.into(VolumeRecord.class);
    }

    @Override
    public List<? extends Volume> findSharedOrUnmappedVolumes(long accountId, String volumeName) {
        List<VolumeRecord> volumes = create()
            .selectDistinct(VOLUME.fields())
            .from(VOLUME)
            .leftOuterJoin(STORAGE_POOL)
                .on(VOLUME.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
            .where(VOLUME.NAME.eq(volumeName)
                .and((VOLUME.REMOVED.isNull())))
                .and(VOLUME.ACCOUNT_ID.eq(accountId))
                .and(STORAGE_POOL.KIND.notIn(LOCAL_POOL_KINDS).or(STORAGE_POOL.KIND.isNull()))
                .and(STORAGE_POOL.REMOVED.isNull())
            .fetchInto(VolumeRecord.class);

        return volumes;
    }

    @Override
    public List<? extends Volume> identifyUnmappedVolumes(long accountId, Set<Long> volumeIds) {
        List<VolumeRecord> volumes = create()
            .selectDistinct(VOLUME.fields())
            .from(VOLUME)
            .where(VOLUME.ID.in(volumeIds)
                .and((VOLUME.REMOVED.isNull())))
                .and(VOLUME.ACCOUNT_ID.eq(accountId))
                .and(VOLUME.STORAGE_POOL_ID.isNull())
            .fetchInto(VolumeRecord.class);
        return volumes;
    }

    public static final List<String> INELLIGIBLE_STATES = Arrays.asList(CommonStatesConstants.ACTIVE, CommonStatesConstants.ACTIVATING,
            CommonStatesConstants.REQUESTED);

    @Override
    public Set<? extends Volume> findNonremovedVolumesWithNoOtherMounts(long instanceId) {
        List<Long> instanceVolumeIds = create()
                .select(MOUNT.VOLUME_ID)
                .from(MOUNT)
                .where(MOUNT.INSTANCE_ID.eq(instanceId))
                .fetchInto(Long.class);

        List<Long> inelligibleVolumeIds = create()
                .select(MOUNT.VOLUME_ID)
                .from(MOUNT)
                .where(MOUNT.VOLUME_ID.in(instanceVolumeIds)
                .and(MOUNT.INSTANCE_ID.ne(instanceId))
                .and(MOUNT.STATE.in(INELLIGIBLE_STATES)))
                .fetchInto(Long.class);

        Set<Long> volumeIds = new HashSet<>(instanceVolumeIds);
        volumeIds.removeAll(inelligibleVolumeIds);

        List<VolumeRecord> vols = create()
                .select(VOLUME.fields())
                .from(VOLUME)
                .where(VOLUME.ID.in(volumeIds))
                .and(VOLUME.REMOVED.isNull())
                .fetchInto(VolumeRecord.class);

        Set<? extends Volume> volumes = new HashSet<Volume>(vols);
        return volumes;
    }

    @Override
    public Volume createVolumeForDriver(final long accountId, final String volumeName, final String driverName) {
        StorageDriver driver = objectManager.findAny(StorageDriver.class,
                STORAGE_DRIVER.NAME, driverName,
                STORAGE_DRIVER.ACCOUNT_ID, accountId,
                STORAGE_DRIVER.REMOVED, null);
        final Long driverId = driver == null ? null : driver.getId();
        return DeferredUtils.nest(new Callable<Volume>() {
            @Override
            public Volume call() throws Exception {
                return resourceDao.createAndSchedule(Volume.class,
                        VOLUME.NAME, volumeName,
                        VOLUME.ACCOUNT_ID, accountId,
                        VOLUME.STORAGE_DRIVER_ID, driverId,
                        VolumeConstants.FIELD_VOLUME_DRIVER, driverName);
            }
        });
    }

    @Override
    public Map<Long, List<MountEntry>> getMountsForInstances(List<Long> ids, final IdFormatter idF) {
        final Map<Long, List<MountEntry>> result = new HashMap<>();
        create().select(INSTANCE.NAME, VOLUME.NAME, VOLUME.ID, MOUNT.PERMISSIONS, MOUNT.PATH, MOUNT.INSTANCE_ID)
            .from(MOUNT)
            .join(VOLUME)
                .on(VOLUME.ID.eq(MOUNT.VOLUME_ID))
            .join(INSTANCE)
                .on(INSTANCE.ID.eq(MOUNT.INSTANCE_ID))
            .where(MOUNT.REMOVED.isNull()
                    .and(VOLUME.REMOVED.isNull())
                    .and(MOUNT.INSTANCE_ID.in(ids)))
            .fetchInto(new RecordHandler<Record6<String, String, Long, String, String, Long>>() {
                @Override
                public void next(Record6<String, String, Long, String, String, Long> record) {
                    Long instanceId = record.getValue(MOUNT.INSTANCE_ID);
                    List<MountEntry> entries = result.get(instanceId);
                    if (entries == null) {
                        entries = new ArrayList<>();
                        result.put(instanceId, entries);
                    }

                    MountEntry mount = new MountEntry();
                    mount.setInstanceName(record.getValue(INSTANCE.NAME));
                    mount.setInstanceId(idF.formatId(InstanceConstants.TYPE, instanceId));
                    mount.setPath(record.getValue(MOUNT.PATH));
                    mount.setPermission(record.getValue(MOUNT.PERMISSIONS));
                    mount.setVolumeId(idF.formatId(VolumeConstants.TYPE, record.getValue(VOLUME.ID)));
                    mount.setVolumeName(record.getValue(VOLUME.NAME));
                    entries.add(mount);
                }
            });

        return result;
    }

    @Override
    public Map<Long, List<MountEntry>> getMountsForVolumes(List<Long> ids, final IdFormatter idF) {
        final Map<Long, List<MountEntry>> result = new HashMap<>();
        create().select(VOLUME.NAME, MOUNT.PERMISSIONS, MOUNT.PATH, MOUNT.INSTANCE_ID, MOUNT.VOLUME_ID, INSTANCE.NAME)
            .from(MOUNT)
            .join(INSTANCE)
                .on(INSTANCE.ID.eq(MOUNT.INSTANCE_ID))
            .join(VOLUME)
                .on(VOLUME.ID.eq(MOUNT.VOLUME_ID))
            .where(INSTANCE.REMOVED.isNull()
                    .and(VOLUME.REMOVED.isNull())
                    .and(MOUNT.VOLUME_ID.in(ids)))
            .fetchInto(new RecordHandler<Record6<String, String, String, Long, Long, String>>() {
                @Override
                public void next(Record6<String, String, String, Long, Long, String> record) {
                    Long volumeId = record.getValue(MOUNT.VOLUME_ID);
                    List<MountEntry> entries = result.get(volumeId);
                    if (entries == null) {
                        entries = new ArrayList<>();
                        result.put(volumeId, entries);
                    }

                    MountEntry mount = new MountEntry();
                    mount.setInstanceName(record.getValue(INSTANCE.NAME));
                    mount.setInstanceId(idF.formatId(InstanceConstants.TYPE, record.getValue(MOUNT.INSTANCE_ID)));
                    mount.setPath(record.getValue(MOUNT.PATH));
                    mount.setPermission(record.getValue(MOUNT.PERMISSIONS));
                    mount.setVolumeId(idF.formatId(VolumeConstants.TYPE, volumeId));
                    mount.setVolumeName(record.getValue(VOLUME.NAME));

                    entries.add(mount);
                }
            });

        return result;
    }

    @Override
    public List<? extends Volume> findNonRemovedVolumesOnPool(Long storagePoolId) {
        return create().select(VOLUME.fields())
                .from(VOLUME)
                .where(VOLUME.REMOVED.isNull()
                        .and(VOLUME.STORAGE_POOL_ID.eq(storagePoolId)))
                .fetchInto(VolumeRecord.class);
    }

    @Override
    public List<Long> findDeploymentUnitsForVolume(Volume volume) {
        if (volume.getVolumeTemplateId() == null) {
            return Collections.emptyList();
        }
        return create().select(DEPLOYMENT_UNIT.ID)
            .from(DEPLOYMENT_UNIT)
            .join(VOLUME_TEMPLATE)
                .on(VOLUME_TEMPLATE.STACK_ID.eq(DEPLOYMENT_UNIT.STACK_ID))
            .where(
                    DEPLOYMENT_UNIT.REMOVED.isNull()
                    .and(VOLUME_TEMPLATE.ID.eq(volume.getVolumeTemplateId())))
            .fetch(DEPLOYMENT_UNIT.ID);
    }

    @Override
    public Long findPoolForVolumeAndHost(Volume volume, long hostId) {
        String storageDriver = DataAccessor.fieldString(volume, VolumeConstants.FIELD_VOLUME_DRIVER);
        Long storageDriverId = volume.getStorageDriverId();

        if (storageDriverId != null) {
            return create().select(STORAGE_POOL.ID)
                .from(STORAGE_POOL)
                .join(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                .where(STORAGE_POOL.STORAGE_DRIVER_ID.eq(storageDriverId)
                        .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull())
                        .and(STORAGE_POOL.REMOVED.isNull())
                        .and(STORAGE_POOL_HOST_MAP.HOST_ID.eq(hostId)))
                .fetchAny(STORAGE_POOL.ID);
        } else if (StringUtils.isBlank(storageDriver) || VolumeConstants.LOCAL_DRIVER.equalsIgnoreCase(storageDriver)) {
            return create().select(STORAGE_POOL.ID)
                .from(STORAGE_POOL)
                .join(STORAGE_POOL_HOST_MAP)
                    .on(STORAGE_POOL_HOST_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                .where(STORAGE_POOL.KIND.in(StoragePoolConstants.UNMANGED_STORAGE_POOLS)
                        .and(STORAGE_POOL_HOST_MAP.REMOVED.isNull())
                        .and(STORAGE_POOL.REMOVED.isNull())
                        .and(STORAGE_POOL_HOST_MAP.HOST_ID.eq(hostId)))
                .fetchAny(STORAGE_POOL.ID);
        }

        return null;
    }
}
