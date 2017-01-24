package io.cattle.platform.process.volume;

import static io.cattle.platform.core.model.tables.StorageDriverTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class VolumeCreateUpdate extends AbstractObjectProcessHandler {

    @Inject
    StoragePoolDao storagePoolDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume) state.getResource();
        String driver = DataAccessor.fieldString(volume, VolumeConstants.FIELD_VOLUME_DRIVER);
        Long driverId = volume.getStorageDriverId();
        Integer deviceNum = volume.getDeviceNumber();
        StorageDriver storageDriver = objectManager.loadResource(StorageDriver.class, driverId);

        if (storageDriver == null && StringUtils.isNotBlank(driver)) {
            storageDriver = objectManager.findAny(StorageDriver.class,
                    STORAGE_DRIVER.ACCOUNT_ID, volume.getAccountId(),
                    STORAGE_DRIVER.REMOVED, null,
                    STORAGE_DRIVER.NAME, driver);
        }

        if (storageDriver != null) {
            driver = storageDriver.getName();
            driverId = storageDriver.getId();
            deviceNum = -1;
        }

        HandlerResult result = new HandlerResult(VolumeConstants.FIELD_VOLUME_DRIVER, driver,
                VOLUME.DEVICE_NUMBER, deviceNum,
                VOLUME.STORAGE_DRIVER_ID, driverId);

        Long hostId = volume.getHostId();
        if (storageDriver != null && hostId != null) {
            if (storagePoolDao.associateVolumeToPool(volume.getId(), storageDriver.getId(), hostId) != null) {
                result.withShouldContinue(false)
                    .withChainProcessName(objectProcessManager.getStandardProcessName(StandardProcess.DEACTIVATE, volume));
            }
        }

        return result;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] {VolumeConstants.PROCESS_CREATE, VolumeConstants.PROCESS_UPDATE};
    }

}