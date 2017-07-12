package io.cattle.platform.process.storagepool;

import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.tables.CredentialTable;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;


public class StoragePoolRemove implements ProcessHandler {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    VolumeDao volumeDao;

    public StoragePoolRemove(ObjectManager objectManager, ObjectProcessManager processManager, VolumeDao volumeDao) {
        super();
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.volumeDao = volumeDao;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        StoragePool registry = (StoragePool) state.getResource();
        if (registry.getKind().equalsIgnoreCase(StoragePoolConstants.KIND_REGISTRY)){
            for (Credential cred : objectManager.find(Credential.class, CredentialTable.CREDENTIAL.REGISTRY_ID,
                    registry.getId(), CredentialTable.CREDENTIAL.REMOVED, null)) {
                try {
                    processManager.executeDeactivateThenScheduleRemove(cred, state.getData());
                } catch (ProcessCancelException e) {
                    //Ignore
                }
            }
            return null;
        }

        for (Volume v : volumeDao.findNonRemovedVolumesOnPool(registry.getId())) {
            processManager.executeDeactivateThenRemove(v, null);
        }

        for (StoragePoolHostMap map : objectManager.children(registry, StoragePoolHostMap.class)) {
            objectManager.delete(map);
        }

        return null;
    }
}
