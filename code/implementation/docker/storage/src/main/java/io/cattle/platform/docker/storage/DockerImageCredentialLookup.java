package io.cattle.platform.docker.storage;

import static io.cattle.platform.core.model.tables.CredentialTable.CREDENTIAL;
import static io.cattle.platform.core.model.tables.StoragePoolTable.STORAGE_POOL;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.storage.ImageCredentialLookup;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;
import javax.inject.Inject;

public class DockerImageCredentialLookup extends AbstractJooqDao implements ImageCredentialLookup {
    @Inject
    DockerStoragePoolDriver dockerStoragePoolDriver;

    @Override
    public Credential getDefaultCredential(String uuid, long currentAccount) {
        Long registryId = null;
        List<StoragePool> storagePools = create().selectFrom(STORAGE_POOL)
                .where(STORAGE_POOL.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(STORAGE_POOL.ACCOUNT_ID.eq(currentAccount)
                        .and(STORAGE_POOL.KIND.eq(StoragePoolConstants.KIND_REGISTRY)))).fetchInto(StoragePool.class);
        for(StoragePool registry: storagePools){
            if (getServer(uuid).equalsIgnoreCase((String) CollectionUtils.getNestedValue(registry.getData(), "fields", StoragePoolConstants.SERVER_ADDRESS))) {
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

    private String getServer(String uuid) {
        String[] splits = dockerStoragePoolDriver.stripKindPrefix(uuid).split("/");
        switch (splits.length) {
            case 1:
                return "index.docker.io";
            default:
                String first = splits[0];
                if (first.contains(".") || first.contains(":") || first.equals("localhost")) {
                    return first;
                } else {
                    return "index.docker.io";
                }
        }
    }
}
