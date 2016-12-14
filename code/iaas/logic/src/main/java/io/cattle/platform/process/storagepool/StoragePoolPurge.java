package io.cattle.platform.process.storagepool;

import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.tables.CredentialTable;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;


@Named
public class StoragePoolPurge extends AbstractDefaultProcessHandler {

    @Inject
    VolumeDao volumeDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        StoragePool registry = (StoragePool) state.getResource();
        if (registry.getKind().equalsIgnoreCase(StoragePoolConstants.KIND_REGISTRY)){
            for (Credential cred : getObjectManager().find(Credential.class, CredentialTable.CREDENTIAL.REGISTRY_ID,
                    registry.getId(), CredentialTable.CREDENTIAL.REMOVED, null)) {
                try {
                    deactivateThenScheduleRemove(cred, state.getData());
                } catch (ProcessCancelException e) {
                    //Ignore
                }
            }
            return null;
        }

        for (Volume v : volumeDao.findNonRemovedVolumesOnPool(registry.getId())) {
            deactivateThenRemove(v, null);
        }

        for (StoragePoolHostMap map : objectManager.children(registry, StoragePoolHostMap.class)) {
            deactivateThenRemove(map, null);
        }

        return null;
    }
}
