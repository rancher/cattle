package io.cattle.platform.process.instance;

import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceDeallocate extends AbstractDefaultProcessHandler {

    @Inject
    AllocatorService allocatorService;
    
    @Inject
    GenericMapDao mapDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        allocatorService.instanceDeallocate(instance);
        return afterDeallocate(state, process, new HashMap<Object, Object>());
    }

    protected HandlerResult afterDeallocate(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Instance instance = (Instance) state.getResource();

        for (InstanceHostMap map : mapDao.findToRemove(InstanceHostMap.class, Instance.class, instance.getId())) {
            remove(map, state.getData());
        }

        return new HandlerResult(result);
    }
}
