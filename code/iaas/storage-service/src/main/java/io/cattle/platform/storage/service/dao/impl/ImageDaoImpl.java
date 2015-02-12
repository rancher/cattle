package io.cattle.platform.storage.service.dao.impl;

import static io.cattle.platform.core.model.tables.ImageStoragePoolMapTable.*;
import static io.cattle.platform.core.model.tables.ImageTable.*;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.ImageStoragePoolMap;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.storage.service.dao.ImageDao;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDaoImpl extends AbstractJooqDao implements ImageDao {

    private static final Logger log = LoggerFactory.getLogger(ImageDaoImpl.class);

    AccountDao accountCoreDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    @Override
    public Image findImageByUuid(String uuid) {
        return objectManager.findOne(Image.class, IMAGE.NAME, uuid);
    }

    @Override
    public Image persistAndAssociateImage(Image image, StoragePool storagePool) {
        image = objectManager.create(image);
        processManager.scheduleStandardProcess(StandardProcess.CREATE, image, null);

        log.info("Registered image [{}] for pool [{}]", image.getId(), storagePool.getId());

        ImageStoragePoolMap map = objectManager.create(ImageStoragePoolMap.class,
                IMAGE_STORAGE_POOL_MAP.IMAGE_ID, image.getId(),
                IMAGE_STORAGE_POOL_MAP.STORAGE_POOL_ID, storagePool.getId());
        processManager.scheduleStandardProcess(StandardProcess.CREATE, map, null);


        return image;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public AccountDao getAccountCoreDao() {
        return accountCoreDao;
    }

    @Inject
    public void setAccountCoreDao(AccountDao accountCoreDao) {
        this.accountCoreDao = accountCoreDao;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

}
