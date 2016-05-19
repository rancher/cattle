package io.cattle.platform.iaas.api.volume;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.SnapshotConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Snapshot;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class VolumeSnapshotActionHandler implements ActionHandler {

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager processManager;

    @Override
    public String getName() {
        return VolumeConstants.PROCESS_SNAPSHOT;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Volume)) {
            return null;
        }
        Volume volume = (Volume)obj;
        Snapshot snapshot = objectManager.newRecord(Snapshot.class);
        snapshot.setKind(SnapshotConstants.TYPE);
        snapshot.setAccountId(volume.getAccountId());
        snapshot.setVolumeId(volume.getId());

        String snapshotName = DataAccessor.fromMap(request.getRequestObject()).withKey("name").as(String.class);
        if (StringUtils.isNotBlank(snapshotName)) {
            snapshot.setName(snapshotName);
        }

        snapshot = objectManager.create(snapshot);
        processManager.scheduleStandardProcess(StandardProcess.CREATE, snapshot, null);
        return objectManager.reload(snapshot);
    }
}