package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.MountTable.*;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.mount.MountDeactivate;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceRemove extends AbstractDefaultProcessHandler {

    InstanceStop instanceStop;
    GenericMapDao mapDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance)state.getResource();

        Map<String, Object> result = new HashMap<String, Object>();

        network(instance, state.getData());

        storage(instance, state.getData());

        return new HandlerResult(result);
    }

    protected void storage(Instance instance, Map<String, Object> data) {
        List<Volume> volumes = getObjectManager().children(instance, Volume.class);

        for (Volume volume : volumes) {
            if (volume.getDeviceNumber() == 0) {
                remove(volume, data);
            } else {
                execute("volume.detach", volume, null);
            }
        }

        Map<Object, Object> criteria = new HashMap<Object, Object>();
        criteria.put(MOUNT.REMOVED, new Condition(ConditionType.NULL));
        criteria.put(MOUNT.STATE, new Condition(ConditionType.NOTIN, MountDeactivate.MOUNT_STATES));
        criteria.put(MOUNT.INSTANCE_ID, instance.getId());
        List<Mount> mounts = getObjectManager().find(Mount.class, criteria);
        for (Mount mount : mounts) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.DEACTIVATE, mount, data);
        }
    }

    protected void network(Instance instance, Map<String, Object> data) {
        List<Nic> nics = getObjectManager().children(instance, Nic.class);

        for (Nic nic : nics) {
            remove(nic, data);
        }
    }

    public InstanceStop getInstanceStop() {
        return instanceStop;
    }

    @Inject
    public void setInstanceStop(InstanceStop instanceStop) {
        this.instanceStop = instanceStop;
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

}
