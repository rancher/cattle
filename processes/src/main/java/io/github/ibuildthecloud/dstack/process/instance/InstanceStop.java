package io.github.ibuildthecloud.dstack.process.instance;

import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.InstanceHostMap;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;

@Named
public class InstanceStop extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance)state.getResource();

        Map<String,Object> result = new ConcurrentHashMap<String,Object>();

        compute(instance);

        storage(instance);

        deallocate(instance);

        return new HandlerResult(result);
    }

    protected void deallocate(Instance instance) {
        ProcessInstance pi = getObjectProcessManager().createProcessInstance("instance.deallocate", instance, null);
        pi.execute();
    }

    protected void storage(Instance instance) {
        List<Volume> volumes = getObjectManager().children(instance, Volume.class);

        for ( Volume volume : volumes ) {
            deactivate(volume, null);
        }
    }

    protected void compute(Instance instance) {
        List<InstanceHostMap> maps = getObjectManager().children(instance, InstanceHostMap.class);

        for ( InstanceHostMap map : maps ) {
            deactivate(map, null);
        }
    }

}
