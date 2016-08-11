package io.cattle.platform.docker.storage;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.docker.constants.DockerStoragePoolConstants;
import io.cattle.platform.docker.storage.dao.DockerStorageDao;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.storage.pool.AbstractKindBasedStoragePoolDriver;
import io.cattle.platform.storage.pool.StoragePoolDriver;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerStoragePoolDriver extends AbstractKindBasedStoragePoolDriver implements StoragePoolDriver {

    private static final Logger log = LoggerFactory.getLogger(DockerStoragePoolDriver.class);

    LockManager lockManager;
    DockerStorageDao storageDao;

    public DockerStoragePoolDriver() {
        super(DockerStoragePoolConstants.DOCKER_KIND);
    }

    @Override
    protected boolean populateImageInternal(String uuid, Image image) {
        image.setName(uuid);

        Map<String, Object> data = image.getData();
        if (data == null) {
            data = new HashMap<>();
            image.setData(data);
        }

        data.put("dockerImage", stripKindPrefix(uuid));
        image.setFormat(DockerStoragePoolConstants.DOCKER_FORMAT);
        image.setInstanceKind(InstanceConstants.KIND_CONTAINER);

        return true;
    }

    public void createDockerExternalPool(final StoragePool parentPool) {
        StoragePool storagePool = storageDao.getExternalStoragePool(parentPool);
        if (storagePool == null) {
            lockManager.lock(new DockerExternalPoolCreateLock(), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    createDockerExternalPoolInternal(parentPool);
                }
            });
        }
    }

    protected void createDockerExternalPoolInternal(StoragePool parentPool) {
        StoragePool storagePool = storageDao.getExternalStoragePool(parentPool);
        if (storagePool != null) {
            return;
        }

        storagePool = storageDao.createExternalStoragePool(parentPool);
        log.info("Created Docker external pool [{}]", storagePool.getId());
    }

    public static boolean isDockerPool(StoragePool pool) {
        return pool == null ? false : DockerStoragePoolConstants.DOCKER_KIND.equals(pool.getKind());
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public DockerStorageDao getStorageDao() {
        return storageDao;
    }

    @Inject
    public void setStorageDao(DockerStorageDao storageDao) {
        this.storageDao = storageDao;
    }

}
