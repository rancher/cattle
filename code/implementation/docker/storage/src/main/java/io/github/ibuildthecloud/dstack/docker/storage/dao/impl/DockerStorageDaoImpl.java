package io.github.ibuildthecloud.dstack.docker.storage.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.StoragePoolTable.*;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.docker.storage.DockerStoragePoolDriver;
import io.github.ibuildthecloud.dstack.docker.storage.dao.DockerStorageDao;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;

public class DockerStorageDaoImpl implements DockerStorageDao {

    ObjectManager objectManager;
    ObjectProcessManager processManager;

    @Override
    public StoragePool getExternalStoragePool() {
        return objectManager.findOne(StoragePool.class,
                STORAGE_POOL.KIND, DockerStoragePoolDriver.DOCKER_KIND,
                STORAGE_POOL.EXTERNAL, true);
    }

    @Override
    public StoragePool createExternalStoragePool() {
        StoragePool externalPool = objectManager.create(StoragePool.class,
                STORAGE_POOL.NAME, "Docker Index",
                STORAGE_POOL.EXTERNAL, true,
                STORAGE_POOL.KIND, DockerStoragePoolDriver.DOCKER_KIND);

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
