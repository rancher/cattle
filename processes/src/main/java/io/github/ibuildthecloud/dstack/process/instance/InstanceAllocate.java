package io.github.ibuildthecloud.dstack.process.instance;

import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.InstanceHostMap;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.core.model.VolumeStoragePoolMap;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.process.common.handler.EventBasedProcessHandler;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

@Named
public class InstanceAllocate extends EventBasedProcessHandler {

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Map<String,Set<Long>> allocationData = new HashMap<String, Set<Long>>();
        result.put("_allocationData", allocationData);

        Instance instance = (Instance)state.getResource();

        for ( InstanceHostMap map : getObjectManager().children(instance, InstanceHostMap.class) ) {
            CollectionUtils.addToMap(allocationData, "instance:" + instance.getId(), map.getHostId(), HashSet.class);
            getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, map, state.getData());
        }

        List<Volume> volumes = getObjectManager().children(instance, Volume.class);

        for ( Volume v : volumes ) {
            getObjectProcessManager().executeStandardProcess(StandardProcess.ALLOCATE, v, state.getData());
        }

        volumes = getObjectManager().children(instance, Volume.class);
        for ( Volume v : volumes ) {
            for ( VolumeStoragePoolMap map : getObjectManager().children(v, VolumeStoragePoolMap.class) ) {
                CollectionUtils.addToMap(allocationData, "volume:" + v.getId(), map.getVolumeId(), HashSet.class);
                getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, map, state.getData());
            }
        }

        return new HandlerResult(result);
    }

}
