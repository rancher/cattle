package io.cattle.platform.docker.storage;

import static io.cattle.platform.core.model.tables.CredentialTable.CREDENTIAL;
import static io.cattle.platform.core.model.tables.StoragePoolTable.STORAGE_POOL;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.storage.ImageCredentialLookup;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;

public class DockerImageCredentialLookup extends AbstractJooqDao implements ImageCredentialLookup {

    @Override
    public Credential getDefaultCredential(String uuid, long currentAccount) {
        DockerImage image = DockerImage.parse(uuid);
        if ( image == null){
            return null;
        }
        String serverAddress = image.getServer();
        Long registryId = null;
        List<StoragePool> storagePools = create().selectFrom(STORAGE_POOL)
                .where(STORAGE_POOL.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(STORAGE_POOL.ACCOUNT_ID.eq(currentAccount)
                        .and(STORAGE_POOL.KIND.eq(StoragePoolConstants.KIND_REGISTRY)))).fetchInto(StoragePool.class);
        for(StoragePool registry: storagePools){
            if (serverAddress.equalsIgnoreCase((String) CollectionUtils.getNestedValue(registry.getData(), "fields", StoragePoolConstants.SERVER_ADDRESS))) {
                registryId = registry.getId();
                break;
            }
        }
        if (registryId == null){
            return null;
        }
        Credential credential = create().selectFrom(CREDENTIAL)
                .where(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(CREDENTIAL.ACCOUNT_ID.eq(currentAccount))
                        .and(CREDENTIAL.REGISTRY_ID.eq(registryId))
                        .and(CREDENTIAL.KIND.eq(CredentialConstants.KIND_REGISTRY_CREDENTIAL)))
                        .orderBy(CREDENTIAL.ID.asc()).fetchAny();

        return credential;
    }
}
