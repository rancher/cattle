package io.github.ibuildthecloud.dstack.storage.service.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.ImageStoragePoolMapTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.ImageTable.*;
import io.github.ibuildthecloud.dstack.core.dao.AccountCoreDao;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.core.model.Image;
import io.github.ibuildthecloud.dstack.core.model.ImageStoragePoolMap;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.storage.service.dao.ImageDao;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDaoImpl extends AbstractJooqDao implements ImageDao {

    private static final Logger log = LoggerFactory.getLogger(ImageDaoImpl.class);

    AccountCoreDao accountCoreDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    @Override
    public Image findImageByUuid(String uuid) {
        return objectManager.findOne(Image.class,
                IMAGE.UUID, uuid);
    }

    @Override
    public Image persistAndAssociateImage(Image image, StoragePool storagePool) {
        Long accountId = image.getAccountId();

        if ( accountId == null ) {
            Account system = accountCoreDao.getSystemAccount();
            if ( system == null ) {
                throw new IllegalStateException("Failed to find system account");
            }
            accountId = system.getId();
        }

        /* Make sure we set the account id through the properties because if
         * we are in the context of the API, it might get overwritten with the
         * user's accountId
         */
        image = objectManager.create(image,
                IMAGE.ACCOUNT_ID, accountId);
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

    public AccountCoreDao getAccountCoreDao() {
        return accountCoreDao;
    }

    @Inject
    public void setAccountCoreDao(AccountCoreDao accountCoreDao) {
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
