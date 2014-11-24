package io.cattle.platform.docker.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.docker.client.DockerClient;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.docker.storage.dao.DockerStorageDao;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.storage.pool.AbstractKindBasedStoragePoolDriver;
import io.cattle.platform.storage.pool.StoragePoolDriver;

public class DockerStoragePoolDriver extends AbstractKindBasedStoragePoolDriver implements StoragePoolDriver {

    public static final String DOCKER_KIND = "docker";
    public static final String DOCKER_FORMAT = "docker";

    private static final Logger log = LoggerFactory.getLogger(DockerStoragePoolDriver.class);

    DockerClient dockerClient;
    LockManager lockManager;
    DockerStorageDao storageDao;

    public DockerStoragePoolDriver() {
        super(DOCKER_KIND);
    }

    @Override
    protected boolean populateExtenalImageInternal(StoragePool pool, String uuid, Image image) throws IOException {
        DockerImage dockerImage = DockerImage.parse(stripKindPrefix(uuid));

        if ( dockerImage == null ) {
            return false;
        }

        dockerImage = dockerClient.lookup(dockerImage);

        if ( dockerImage == null ) {
            return false;
        }

        image.setName(dockerImage.getFullName());

        Map<String,Object> data = image.getData();
        if ( data == null ) {
            data = new HashMap<String, Object>();
            image.setData(data);
        }

        data.put("dockerImage", dockerImage);
        image.setFormat(DOCKER_FORMAT);
        image.setInstanceKind(InstanceConstants.KIND_CONTAINER);

        return true;
    }

    public void createDockerExternalPool(final StoragePool parentPool) {
        StoragePool storagePool = storageDao.getExternalStoragePool(parentPool);
        if ( storagePool == null ) {
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
        if ( storagePool != null ) {
            return;
        }

        storagePool = storageDao.createExternalStoragePool(parentPool);
        log.info("Created Docker external pool [{}]", storagePool.getId());
    }

    public static boolean isDockerPool(StoragePool pool) {
        return pool == null ? false : DOCKER_KIND.equals(pool.getKind());
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }

    @Inject
    public void setDockerClient(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
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
