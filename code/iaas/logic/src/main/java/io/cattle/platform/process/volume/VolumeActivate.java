package io.cattle.platform.process.volume;

import static io.cattle.platform.core.model.tables.ImageStoragePoolMapTable.*;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.ImageStoragePoolMap;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.lock.ImageAssociateLock;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VolumeActivate extends AbstractDefaultProcessHandler {

    GenericMapDao mapDao;
    LockManager lockManager;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume) state.getResource();

        Set<Long> pools = new HashSet<Long>();
        for (VolumeStoragePoolMap map : mapDao.findNonRemoved(VolumeStoragePoolMap.class, Volume.class, volume.getId())) {
            activatePool(volume, map, state.getData());
            pools.add(map.getStoragePoolId());
        }

        return new HandlerResult("_activatedPools", pools);
    }

    protected void activatePool(Volume volume, VolumeStoragePoolMap map, Map<String, Object> data) {
        Image image = getObjectManager().loadResource(Image.class, volume.getImageId());

        activateImageInPool(volume, image, map.getStoragePoolId(), data);
        createThenActivate(map, data);
    }

    protected void activateImageInPool(Volume volume, final Image image, final long poolId, Map<String, Object> data) {
        if (image == null) {
            return;
        }

        activate(image, data);

        ImageStoragePoolMap map = getMap(image, poolId);
        if (map == null) {
            map = lockManager.lock(new ImageAssociateLock(image.getId(), poolId), new LockCallback<ImageStoragePoolMap>() {
                @Override
                public ImageStoragePoolMap doWithLock() {
                    return associate(image, poolId);
                }
            });
        }

        create(map, data);
        activate(map, data);
    }

    protected ImageStoragePoolMap associate(Image image, long poolId) {
        ImageStoragePoolMap map = getMap(image, poolId);
        if (map == null) {
            map = getObjectManager().create(ImageStoragePoolMap.class, IMAGE_STORAGE_POOL_MAP.STORAGE_POOL_ID, poolId, IMAGE_STORAGE_POOL_MAP.IMAGE_ID,
                    image.getId());
        }

        return map;
    }

    protected ImageStoragePoolMap getMap(Image image, long poolId) {
        return mapDao.findNonRemoved(ImageStoragePoolMap.class, Image.class, image.getId(), StoragePool.class, poolId);
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }
}