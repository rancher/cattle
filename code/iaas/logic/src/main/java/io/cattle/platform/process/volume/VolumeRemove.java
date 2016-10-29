package io.cattle.platform.process.volume;

import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.instance.IgnoreReconnectionAgentHandler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VolumeRemove extends IgnoreReconnectionAgentHandler {

    @Inject
    GenericMapDao mapDao;

    public VolumeRemove() {
        setProcessNames(new String[] { VolumeConstants.PROCESS_REMOVE });
    }

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Volume v = (Volume)state.getResource();
        return objectManager.loadResource(Host.class, v.getHostId());
    }

    @Override
    protected Object getEventResource(ProcessState state, ProcessInstance process) {
        return getDataResource(state, process);
    }

    @Override
    protected Object getDataResource(ProcessState state, ProcessInstance process) {
        Volume v = (Volume)state.getResource();
        List<? extends VolumeStoragePoolMap> maps = objectManager.children(v, VolumeStoragePoolMap.class);
        return maps.size() > 0 ? maps.get(0) : null;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Volume volume = (Volume)state.getResource();

        deallocate(volume, state.getData());

        boolean maps = false;
        boolean mounts = false;

        for (VolumeStoragePoolMap map : mapDao.findToRemove(VolumeStoragePoolMap.class, Volume.class, volume.getId())) {
            maps = true;
            deactivateThenRemove(map, null);
        }

        for (Mount mount : mapDao.findToRemove(Mount.class, Volume.class, volume.getId())) {
            mounts = true;
            deactivateThenRemove(mount, null);
        }

        if (maps && !mounts && volume.getHostId() != null) {
            /* HostId != null means this volume was created from a host, but no mounts also means it won't be deleted,
             * so we delete.
             */
            return super.handle(state, process);
        }

        return new HandlerResult();
    }


}
