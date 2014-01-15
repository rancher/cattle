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
public class InstanceStart extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance)state.getResource();

        Map<String,Object> result = new ConcurrentHashMap<String,Object>();

        allocate(instance);

        storage(instance);

        compute(instance);

        return new HandlerResult(result);
    }

    protected void allocate(Instance instance) {
        ProcessInstance pi = getObjectProcessManager().createProcessInstance("instance.allocate", instance, null);
        pi.execute();
    }

    protected void storage(Instance instance) {
        List<Volume> volumes = getObjectManager().children(instance, Volume.class);

        for ( Volume volume : volumes ) {
            activate(volume, null);
        }
    }

    protected void compute(Instance instance) {
        List<InstanceHostMap> maps = getObjectManager().children(instance, InstanceHostMap.class);

        for ( InstanceHostMap map : maps ) {
            activate(map, null);
        }
    }

}
