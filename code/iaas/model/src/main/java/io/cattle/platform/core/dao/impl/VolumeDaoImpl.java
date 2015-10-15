package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.core.model.tables.records.VolumeRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.jooq.Record;

public class VolumeDaoImpl extends AbstractJooqDao implements VolumeDao {

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public Volume findVolumeByExternalId(Long storagePoolId, String externalId) {
        Record record = create()
            .select(VOLUME.fields())
            .from(VOLUME)
            .join(VOLUME_STORAGE_POOL_MAP)
            .on(VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(storagePoolId))
            .where(VOLUME.EXTERNAL_ID.eq(externalId)
            .and((VOLUME.REMOVED.isNull().or(VOLUME.STATE.eq(CommonStatesConstants.REMOVING)))))
            .fetchAny();
        
        return record == null ? null : record.into(VolumeRecord.class);
    }

    @Override
    public void createVolumeInStoragePool(Map<String, Object> volumeData, StoragePool storagePool) {
        Volume volume = resourceDao.createAndSchedule(Volume.class, volumeData);
        Map<String, Object> vspm = new HashMap<String, Object>();
        vspm.put("volumeId", volume.getId());
        vspm.put("storagePoolId", storagePool.getId());
        resourceDao.createAndSchedule(VolumeStoragePoolMap.class, vspm);
    }
}
