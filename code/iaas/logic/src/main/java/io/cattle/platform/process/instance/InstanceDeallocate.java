package io.cattle.platform.process.instance;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.EventBasedProcessHandler;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceDeallocate extends EventBasedProcessHandler {

    GenericMapDao mapDao;

    public InstanceDeallocate() {
        setPriority(DEFAULT);
    }

    @Override
    protected HandlerResult postEvent(ProcessState state, ProcessInstance process, Map<Object, Object> result) {
        Instance instance = (Instance) state.getResource();

        for (InstanceHostMap map : mapDao.findToRemove(InstanceHostMap.class, Instance.class, instance.getId())) {
            remove(map, state.getData());
        }

        return new HandlerResult(result);
    }

    public GenericMapDao getMapDao() {
        return mapDao;
    }

    @Inject
    public void setMapDao(GenericMapDao mapDao) {
        this.mapDao = mapDao;
    }

}
