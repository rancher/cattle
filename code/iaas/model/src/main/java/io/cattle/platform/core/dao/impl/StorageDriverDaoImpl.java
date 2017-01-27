package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.StorageDriverTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.StorageDriverConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.StorageDriverDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Hex;

@Named
public class StorageDriverDaoImpl extends AbstractJooqDao implements StorageDriverDao {

    @Inject
    ObjectManager objectManager;
    @Inject
    VolumeDao volumeDao;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public StorageDriver findSecretsDriver(long accountId) {
        for (StorageDriver driver : objectManager.find(StorageDriver.class,
                STORAGE_DRIVER.ACCOUNT_ID, accountId,
                STORAGE_DRIVER.REMOVED, null)) {
            List<String> caps = DataAccessor.fieldStringList(driver, StorageDriverConstants.FIELD_VOLUME_CAPABILITES);
            if (caps != null && caps.contains(StorageDriverConstants.CAPABILITY_SECRETS)) {
                return driver;
            }
        }

        return null;
    }

    @Override
    public Volume createSecretsVolume(Instance instance, StorageDriver storageDriver, String token) {
        Map<String, Object> dataVolumesMounts = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS);
        Object volumeId = dataVolumesMounts.get(VolumeConstants.SECRETS_PATH);
        if (volumeId != null) {
            return objectManager.loadResource(Volume.class, volumeId.toString());
        }

        byte[] bytes = new byte[32];
        ThreadLocalRandom.current().nextBytes(bytes);
        String name = Hex.encodeHexString(bytes);

        Map<String, Object> tokenMap = CollectionUtils.asMap("value", token);
        Volume volume;
        try {
            volume = resourceDao.create(Volume.class,
                    VOLUME.NAME, name,
                    VOLUME.ACCOUNT_ID, instance.getAccountId(),
                    VOLUME.STORAGE_DRIVER_ID, storageDriver.getId(),
                    VolumeConstants.FIELD_VOLUME_DRIVER, storageDriver.getName(),
                    VolumeConstants.FIELD_VOLUME_DRIVER_OPTS, CollectionUtils.asMap(
                            VolumeConstants.SECRETS_OPT_KEY, jsonMapper.writeValueAsString(tokenMap)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        dataVolumesMounts.put(VolumeConstants.SECRETS_PATH, volume.getId());
        objectManager.setFields(instance, InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, dataVolumesMounts);

        return volume;
    }

}
