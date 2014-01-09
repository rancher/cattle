package io.github.ibuildthecloud.dstack.process.volume;

import static io.github.ibuildthecloud.dstack.core.model.tables.ImageStoragePoolMapTable.*;
import io.github.ibuildthecloud.dstack.core.dao.ImageStoragePoolMapDao;
import io.github.ibuildthecloud.dstack.core.model.Image;
import io.github.ibuildthecloud.dstack.core.model.ImageStoragePoolMap;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.core.model.VolumeStoragePoolMap;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VolumeActivate extends AbstractDefaultProcessHandler {

    ImageStoragePoolMapDao imageStoragePoolMapDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume)state.getResource();

        Set<Long> pools = new HashSet<Long>();
        for ( VolumeStoragePoolMap map : getObjectManager().children(volume, VolumeStoragePoolMap.class) ) {
            if ( map.getRemoved() == null ) {
                activatePool(volume, map, state.getData());
                pools.add(map.getStoragePoolId());
            }
        }

        return new HandlerResult("_activatedPools", pools);
    }

    protected void activatePool(Volume volume, VolumeStoragePoolMap map, Map<String,Object> data) {
        Image image = getObjectManager().loadResource(Image.class, volume.getImageId());

        activateImageInPool(volume, image, map.getStoragePoolId(), data);
        activate(map, data);
    }

    protected void activateImageInPool(Volume volume, Image image, long poolId, Map<String,Object> data) {
        if ( image == null ) {
            return;
        }

        activate(image, data);

        ImageStoragePoolMap map = imageStoragePoolMapDao.findNonRemovedMap(image.getId(), poolId);
        if ( map == null ) {
            map = getObjectManager().create(ImageStoragePoolMap.class,
                    IMAGE_STORAGE_POOL_MAP.STORAGE_POOL_ID, poolId,
                    IMAGE_STORAGE_POOL_MAP.IMAGE_ID, image.getId());
        }

        create(map, data);
        activate(map, data);
    }

    public ImageStoragePoolMapDao getImageStoragePoolMapDao() {
        return imageStoragePoolMapDao;
    }

    @Inject
    public void setImageStoragePoolMapDao(ImageStoragePoolMapDao imageStoragePoolMapDao) {
        this.imageStoragePoolMapDao = imageStoragePoolMapDao;
    }
}