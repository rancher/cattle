package io.cattle.platform.process.instance;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;

@Named
public class InstanceRestore extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance)state.getResource();

        Map<String,Object> result = new ConcurrentHashMap<String,Object>();

        network(instance, state.getData());

        storage(instance, state.getData());

        return new HandlerResult(result);
    }

    protected void storage(Instance instance, Map<String,Object> data) {
        List<Volume> volumes = getObjectManager().children(instance, Volume.class);

        for ( Volume volume : volumes ) {
            restore(volume, data);
        }
    }

    protected void network(Instance instance, Map<String,Object> data) {
        List<Nic> nics = getObjectManager().children(instance, Nic.class);

        for ( Nic nic : nics ) {
            restore(nic, data);
        }
    }

}
