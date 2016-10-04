package io.cattle.platform.process.driver;

import io.cattle.platform.core.constants.StorageDriverConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class StorageDriverActivate extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        StorageDriver storageDriver = (StorageDriver)state.getResource();
        String scope = DataAccessor.fieldString(storageDriver, StorageDriverConstants.FIELD_SCOPE);
        if (!StorageDriverConstants.VALID_SCOPES.contains(scope)) {
            scope = StorageDriverConstants.SCOPE_ENVIRONMENT;
        }
        String accessMode = DataAccessor.fieldString(storageDriver, StorageDriverConstants.FIELD_VOLUME_ACCESS_MODE);
        if (!VolumeConstants.VALID_ACCESS_MODES.contains(accessMode)) {
            accessMode = VolumeConstants.DEFAULT_ACCESS_MODE;
        }

        return new HandlerResult(StorageDriverConstants.FIELD_SCOPE, scope,
                StorageDriverConstants.FIELD_VOLUME_ACCESS_MODE, accessMode);
    }

}