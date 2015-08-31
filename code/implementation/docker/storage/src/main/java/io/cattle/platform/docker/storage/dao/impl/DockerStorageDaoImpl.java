package io.cattle.platform.docker.storage.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ImageTable.*;
import static io.cattle.platform.core.model.tables.StoragePoolTable.*;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.docker.constants.DockerStoragePoolConstants;
import io.cattle.platform.docker.storage.dao.DockerStorageDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.storage.ImageCredentialLookup;
import io.cattle.platform.storage.service.StorageService;

import java.util.List;

import javax.inject.Inject;

public class DockerStorageDaoImpl implements DockerStorageDao {

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager processManager;

    @Inject
    StorageService storageService;

    List<ImageCredentialLookup> imageCredentialLookups;

    @Override
    public StoragePool getExternalStoragePool(StoragePool parentPool) {
        return objectManager.findOne(StoragePool.class,
                STORAGE_POOL.KIND, DockerStoragePoolConstants.DOCKER_KIND,
                STORAGE_POOL.EXTERNAL, true);
    }

    @Override
    public StoragePool createExternalStoragePool(StoragePool parentPool) {
        StoragePool externalPool = objectManager.create(StoragePool.class,
                STORAGE_POOL.NAME, "Docker Index",
                STORAGE_POOL.ACCOUNT_ID, parentPool.getAccountId(),
                STORAGE_POOL.EXTERNAL, true,
                STORAGE_POOL.KIND, DockerStoragePoolConstants.DOCKER_KIND);

        processManager.scheduleStandardProcess(StandardProcess.CREATE, externalPool, null);
        return objectManager.reload(externalPool);
    }

    @Override
    public Image createImageForInstance(Instance instance) {
        String uuid = (String) DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_IMAGE_UUID).get();
        Image image = storageService.registerRemoteImage(uuid);
        if (image != null) {
            objectManager.setFields(instance, INSTANCE.IMAGE_ID, image.getId());
            long currentAccount = instance.getAccountId();
            Long id = instance.getRegistryCredentialId();
            image = objectManager.loadResource(Image.class, instance.getImageId());
            if (id == null) {
                for (ImageCredentialLookup imageLookup: imageCredentialLookups){
                    Credential cred = imageLookup.getDefaultCredential(uuid, currentAccount);
                    if (cred == null){
                        continue;
                    }
                    if (cred.getId() != null){
                        objectManager.setFields(instance, INSTANCE.REGISTRY_CREDENTIAL_ID, cred.getId());
                        break;
                    }
                }
            }
            if (instance.getRegistryCredentialId() != null) {
                objectManager.setFields(image, IMAGE.REGISTRY_CREDENTIAL_ID, instance.getRegistryCredentialId());
            }
        }

        return image;
    }

    public List<ImageCredentialLookup> getImageCredentialLookups() {
        return imageCredentialLookups;
    }

    @Inject
    public void setImageCredentialLookups(List<ImageCredentialLookup> imageCredentialLookups) {
        this.imageCredentialLookups = imageCredentialLookups;
    }
}
