package io.cattle.platform.process.driver;

import io.cattle.platform.core.constants.StorageDriverConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.HashSet;
import java.util.List;

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

        List<String> capabilities = DataAccessor.fieldStringList(storageDriver, ObjectMetaDataManager.CAPABILITIES_FIELD);
        if (StorageDriverConstants.SCOPE_LOCAL.equals(scope)) {
            if(!new HashSet<String>(capabilities).contains(StorageDriverConstants.CAPABILITY_SCHEDULE_SIZE)) {
                capabilities.add(StorageDriverConstants.CAPABILITY_SCHEDULE_SIZE);
            }
        }

        return new HandlerResult(StorageDriverConstants.FIELD_SCOPE, scope,
                StorageDriverConstants.FIELD_VOLUME_ACCESS_MODE, accessMode,
                ObjectMetaDataManager.CAPABILITIES_FIELD, capabilities);
    }

}