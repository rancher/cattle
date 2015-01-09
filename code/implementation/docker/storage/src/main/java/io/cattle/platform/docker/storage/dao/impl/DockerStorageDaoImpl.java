package io.cattle.platform.docker.storage.dao.impl;

import static io.cattle.platform.core.model.tables.StoragePoolTable.*;

import javax.inject.Inject;

import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.docker.storage.DockerStoragePoolDriver;
import io.cattle.platform.docker.storage.dao.DockerStorageDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;

public class DockerStorageDaoImpl implements DockerStorageDao {

    ObjectManager objectManager;
    ObjectProcessManager processManager;

    @Override
    public StoragePool getExternalStoragePool(StoragePool parentPool) {
        return objectManager.findOne(StoragePool.class, STORAGE_POOL.KIND, DockerStoragePoolDriver.DOCKER_KIND, STORAGE_POOL.EXTERNAL, true);
    }

    @Override
    public StoragePool createExternalStoragePool(StoragePool parentPool) {
        StoragePool externalPool = objectManager.create(StoragePool.class, STORAGE_POOL.NAME, "Docker Index", STORAGE_POOL.ACCOUNT_ID,
                parentPool.getAccountId(), STORAGE_POOL.EXTERNAL, true, STORAGE_POOL.KIND, DockerStoragePoolDriver.DOCKER_KIND);

        processManager.scheduleStandardProcess(StandardProcess.CREATE, externalPool, null);
        return objectManager.reload(externalPool);
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
