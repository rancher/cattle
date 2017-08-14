package io.cattle.platform.process.volume;

import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.cattle.platform.core.model.tables.StorageDriverTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;

public class VolumeProcessManager {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    StoragePoolDao storagePoolDao;
    VolumeDao volumeDao;

    public VolumeProcessManager(ObjectManager objectManager, ObjectProcessManager processManager, StoragePoolDao storagePoolDao, VolumeDao volumeDao) {
        super();
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.storagePoolDao = storagePoolDao;
        this.volumeDao = volumeDao;
    }

    private void preCreate(Volume v, Map<Object, Object> data) {
        String driver = DataAccessor.fieldString(v, VolumeConstants.FIELD_VOLUME_DRIVER);
        if (StringUtils.isEmpty(driver) || VolumeConstants.LOCAL_DRIVER.equals(driver)) {
            return;
        }

        List<? extends StoragePool> pools = storagePoolDao.findStoragePoolByDriverName(v.getClusterId(), driver);
        if (pools.size() == 0) {
            return;
        }
        StoragePool sp = pools.get(0);

        List<String> volumeCap = DataAccessor.fieldStringList(sp, StoragePoolConstants.FIELD_VOLUME_CAPABILITIES);
        if (volumeCap != null && volumeCap.size() > 0) {
            data.put(ObjectMetaDataManager.CAPABILITIES_FIELD, volumeCap);
        }
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        Map<Object, Object> data = new HashMap<>();
        Volume volume = (Volume) state.getResource();

        preCreate(volume, data);

        String driver = DataAccessor.fieldString(volume, VolumeConstants.FIELD_VOLUME_DRIVER);
        Long driverId = volume.getStorageDriverId();
        StorageDriver storageDriver = objectManager.loadResource(StorageDriver.class, driverId);

        if (storageDriver == null && StringUtils.isNotBlank(driver)) {
            storageDriver = objectManager.findAny(StorageDriver.class,
                    STORAGE_DRIVER.CLUSTER_ID, volume.getClusterId(),
                    STORAGE_DRIVER.REMOVED, null,
                    STORAGE_DRIVER.NAME, driver);
        }

        if (storageDriver != null) {
            driver = storageDriver.getName();
            driverId = storageDriver.getId();
        }

        data.put(VolumeConstants.FIELD_VOLUME_DRIVER, driver);
        data.put(VOLUME.STORAGE_DRIVER_ID, driverId);

        HandlerResult result = new HandlerResult(data);

        Long hostId = volume.getHostId();
        if (storageDriver != null && hostId != null) {
            if (storagePoolDao.associateVolumeToPool(volume.getId(), storageDriver.getId(), hostId) != null) {
                result.withChainProcessName(processManager.getStandardProcessName(StandardProcess.DEACTIVATE, volume));
            }
        }

        return result;
    }

    public HandlerResult update(ProcessState state, ProcessInstance process) {
        return create(state, process);
    }

    public HandlerResult remove(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume)state.getResource();

        for (Mount mount : volumeDao.findMountsToRemove(volume.getId())) {
            processManager.executeDeactivateThenRemove(mount, null);
        }

        return null;
    }

}
