package io.cattle.platform.docker.process.instance;

import static io.cattle.platform.core.model.tables.ImageTable.IMAGE;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.docker.constants.DockerNetworkConstants;
import io.cattle.platform.docker.constants.DockerStoragePoolConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.storage.ImageCredentialLookup;
import io.cattle.platform.storage.service.StorageService;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

public class DockerInstancePreCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    NetworkDao networkDao;
    @Inject
    StorageService storageService;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;
    List<ImageCredentialLookup> imageCredentialLookups;

    @Override
    public String[] getProcessNames() {
        return new String[]{"instance.create"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!InstanceConstants.KIND_CONTAINER.equals(instance.getKind())) {
            return null;
        }
        createImageAndPickCredential(instance);
        String mode = DataAccessor.fieldString(instance, DockerInstanceConstants.FIELD_NETWORK_MODE);
        String kind = DockerNetworkConstants.MODE_TO_KIND.get(mode);

        if (mode == null || kind == null) {
            return null;
        }

        Network network = networkDao.getNetworkForObject(instance, kind);
        if (network == null) {
            return null;
        }

        return new HandlerResult(InstanceConstants.FIELD_NETWORK_IDS, Arrays.asList(network.getId())).withShouldContinue(true);
    }

    private void createImageAndPickCredential(Instance instance) {
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
    }


    public List<ImageCredentialLookup> imageCredentialLookups() {
        return imageCredentialLookups;
    }

    @Inject
    public void setImageCredentialLookups(List<ImageCredentialLookup> imageCredentialLookups) {
        this.imageCredentialLookups = imageCredentialLookups;
    }
}
