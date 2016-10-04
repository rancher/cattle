package io.cattle.platform.process.volume;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.common.util.ProcessUtils;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VolumeRemove extends AbstractDefaultProcessHandler {

    GenericMapDao mapDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume)state.getResource();

        deallocate(volume, state.getData());

        for (VolumeStoragePoolMap map : mapDao.findToRemove(VolumeStoragePoolMap.class, Volume.class, volume.getId())) {
            try {
                objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, map,
                        ProcessUtils.chainInData(state.getData(), "volumestoragepoolmap.deactivate", "volumestoragepoolmap.remove"));
            } catch (ProcessCancelException e) {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, map, null);
            }
        }

        for (Mount mount : mapDao.findToRemove(Mount.class, Volume.class, volume.getId())) {
            try {
                objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, mount,
                        ProcessUtils.chainInData(state.getData(), "mount.deactivate", "mount.remove"));
            } catch (ProcessCancelException e) {
                objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, mount, null);
            }
        }

        return new HandlerResult();
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

}
