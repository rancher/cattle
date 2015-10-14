package io.cattle.platform.process.storagepool;

import io.cattle.platform.core.constants.StoragePoolConstants;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.tables.CredentialTable;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class StoragePoolPurge extends AbstractDefaultProcessHandler {


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
        } else {
            return null;
        }
    }
}
