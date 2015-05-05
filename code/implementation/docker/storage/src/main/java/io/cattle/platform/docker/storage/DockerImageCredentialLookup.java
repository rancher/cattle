package io.cattle.platform.docker.storage;

import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.storage.ImageCredentialLookup;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;

public class DockerImageCredentialLookup implements ImageCredentialLookup {

    private static final String REGISTRY = "registry";
    private static final String REGISTRY_CREDENTIAL = "registryCredential";

    @Override
    public Credential getDefaultCredential(String uuid, List<?> storagePools, List<?> credentials) {
        DockerImage image = DockerImage.parse(uuid);
        if ( image == null){
            return null;
        }
        String serverAddress = image.getServer();
        Long registryId = null;
        for(Object item: storagePools){
            if (!(item instanceof StoragePool)) {
                continue;
            }
            StoragePool registry = ((StoragePool) item);
            if (!(registry.getKind().equalsIgnoreCase(REGISTRY))){
                continue;
            }
            if (serverAddress.equalsIgnoreCase((String) CollectionUtils.getNestedValue(registry.getData(), "fields", "serverAddress"))) {
                registryId = registry.getId();
                break;
            }
        }
        Credential toReturn = null;
        if (registryId == null){
            return null;
        }
        for(Object cred: credentials){
            if (!(cred instanceof Credential)){
                continue;
            }
            Credential credential = (Credential) cred;
            if (!credential.getKind().equalsIgnoreCase(REGISTRY_CREDENTIAL)){
                continue;
            }
            if (credential.getRegistryId().equals(registryId)){
                if (toReturn == null){
                    toReturn = credential;
                } else if(credential.getId() < toReturn.getId()){
                    toReturn = credential;
                }
            }
        }

        return toReturn;
    }
}
