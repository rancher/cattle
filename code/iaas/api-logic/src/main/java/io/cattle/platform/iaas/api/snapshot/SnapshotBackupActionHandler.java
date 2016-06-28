package io.cattle.platform.iaas.api.snapshot;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.SnapshotConstants;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

public class SnapshotBackupActionHandler implements ActionHandler {

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager processManager;

    @Override
    public String getName() {
        return SnapshotConstants.PROCESS_BACKUP;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Snapshot)) {
            return null;
        }
        Snapshot snapshot = (Snapshot)obj;
        Long backupTargetId = DataAccessor.fromMap(request.getRequestObject()).withKey("backupTargetId").as(Long.class);
        snapshot.setBackupTargetId(backupTargetId);
        snapshot = objectManager.persist(snapshot);
        processManager.scheduleProcessInstance(SnapshotConstants.PROCESS_BACKUP, snapshot, null);
        return objectManager.reload(snapshot);
        
    }
}