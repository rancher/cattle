package io.cattle.platform.process.instance;

import static io.cattle.platform.core.model.tables.MountTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Mount;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.mount.MountDeactivate;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class InstanceRemove extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance)state.getResource();

        Map<String, Object> result = new HashMap<String, Object>();

        network(instance, state.getData());

        storage(instance, state.getData());

        setRemoveSource(instance, state);

        return new HandlerResult(result);
    }

    protected void setRemoveSource(Instance instance, ProcessState state) {
        String removeSource = DataAccessor.fromMap(state.getData()).withKey(InstanceConstants.FIELD_REMOVE_SOURCE)
                .withDefault("").as(String.class);
        if (StringUtils.isEmpty(removeSource)) {
            return;
        }
        String currentValue = DataAccessor.fieldString(instance, InstanceConstants.FIELD_REMOVE_SOURCE);
        if (removeSource.equalsIgnoreCase(currentValue)) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put(InstanceConstants.FIELD_REMOVE_SOURCE, removeSource);
        objectManager.setFields(instance, data);
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
}
