package io.cattle.platform.process.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceStop extends AbstractDefaultProcessHandler {

    GenericMapDao mapDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance)state.getResource();

        Map<String,Object> result = new ConcurrentHashMap<String,Object>();

        compute(instance);

        network(instance);

        storage(instance);

        deallocate(instance);

        if ( Boolean.TRUE.equals(state.getData().get(InstanceConstants.REMOVE_OPTION))) {
            getObjectProcessManager().scheduleStandardProcess(StandardProcess.REMOVE, instance, state.getData());
        }

        return new HandlerResult(result);
    }

    protected void deallocate(Instance instance) {
        deallocate(instance, null);
    }

    protected void storage(Instance instance) {
        List<Volume> volumes = getObjectManager().children(instance, Volume.class);

        for ( Volume volume : volumes ) {
            deactivate(volume, null);
        }
    }

    protected void network(Instance instance) {
        List<Nic> nics = getObjectManager().children(instance, Nic.class);

        for ( Nic nic : nics ) {
            deactivate(nic, null);
        }
    }

    protected void compute(Instance instance) {
        for ( InstanceHostMap map : mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId()) ) {
            if ( map.getRemoved() == null ) {
                deactivate(map, null);
            }
        }
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

}
