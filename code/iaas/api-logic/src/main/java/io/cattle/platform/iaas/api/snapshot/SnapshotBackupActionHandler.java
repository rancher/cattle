package io.cattle.platform.iaas.api.snapshot;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Backup;
import io.cattle.platform.core.model.BackupTarget;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class SnapshotBackupActionHandler implements ActionHandler {

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager processManager;

    @Override
    public String getName() {
        return "snapshot.backup";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Snapshot)) {
            return null;
        }

        Long targetId = DataAccessor.fromMap(request.getRequestObject()).withKey("backupTargetId").as(Long.class);
        BackupTarget target = objectManager.loadResource(BackupTarget.class, targetId);
        if (!CommonStatesConstants.CREATED.equalsIgnoreCase(target.getState())) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, ValidationErrorCodes.INVALID_REFERENCE,
                    "BackupTarget must be in created state.", null);
        }

        Snapshot snapshot = (Snapshot)obj;
        Backup backup = objectManager.newRecord(Backup.class);

        String backupName = DataAccessor.fromMap(request.getRequestObject()).withKey("name").as(String.class);
        if (StringUtils.isNotBlank(backupName)) {
            backup.setName(backupName);
        }

        backup.setKind("backup");
        backup.setAccountId(snapshot.getAccountId());
        backup.setSnapshotId(snapshot.getId());
        backup.setVolumeId(snapshot.getVolumeId());
        backup.setBackupTargetId(targetId);
        backup = objectManager.create(backup);
        processManager.scheduleStandardProcess(StandardProcess.CREATE, backup, null);
        return objectManager.reload(backup);
    }
}