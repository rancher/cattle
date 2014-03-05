package io.github.ibuildthecloud.dstack.process.instance;

import io.github.ibuildthecloud.dstack.core.constants.InstanceConstants;
import io.github.ibuildthecloud.dstack.core.dao.GenericMapDao;
import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.InstanceHostMap;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;

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
